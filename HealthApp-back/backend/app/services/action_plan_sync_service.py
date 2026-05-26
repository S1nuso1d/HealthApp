"""
План на сегодня: персональный подбор задач и автоматическое выполнение при достижении целей.
"""

from __future__ import annotations

import random
from dataclasses import dataclass
from datetime import date, datetime, timezone
from typing import Any

from sqlalchemy.orm import Session

from app.models.action_plan import ActionPlan
from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.recommendations.recommendation_engine import RecommendationEngine
from app.services.analytics.daily_summary_service import DailySummaryService
from app.services.personalized_advisor import PersonalizedAdvisor

_WALK = frozenset({"walk", "walking", "ходьба"})
_PRIORITY_SCORE = {"high": 3, "medium": 2, "low": 1}

# Ротация заголовков, если персональных рекомендаций мало
_FALLBACK_TASKS: dict[str, list[dict[str, str]]] = {
    "hydration": [
        {
            "title": "Вода до обеда",
            "description": "Добавьте 400–500 мл до основного приёма пищи — так проще закрыть дневную цель.",
            "action": "Запишите стакан воды в дневник гидратации.",
        },
        {
            "title": "Стакан воды сейчас",
            "description": "Короткая пауза на воду помогает держать энергию и концентрацию.",
            "action": "Отметьте 250 мл в приложении.",
        },
        {
            "title": "Проверить прогресс по воде",
            "description": "Сверьте остаток до цели и распределите объём на вторую половину дня.",
            "action": "Откройте раздел «Вода» и добавьте запись.",
        },
    ],
    "activity": [
        {
            "title": "Прогулка 15 минут",
            "description": "Короткая активность поднимает шаги и расход калорий без перегруза.",
            "action": "Пройдитесь после обеда или перед ужином.",
        },
        {
            "title": "Поднять шаги сегодня",
            "description": "Добавьте 2000–3000 шагов к текущему результату — это заметный вклад в цель.",
            "action": "Запланируйте выход из транспорта на 1–2 остановки раньше.",
        },
        {
            "title": "Лёгкая тренировка",
            "description": "20 минут умеренной нагрузки улучшат сон и метаболизм.",
            "action": "Запишите тренировку в дневник активности.",
        },
    ],
    "sleep": [
        {
            "title": "Подготовка ко сну",
            "description": "За 60 минут до сна снизьте яркий свет и экраны — так проще уложиться в цель по часам.",
            "action": "Запланируйте отбой и отметьте сон утром.",
        },
        {
            "title": "Стабильный отбой",
            "description": "Ложитесь в одно и то же окно ±30 мин — качество сна обычно растёт.",
            "action": "Поставьте напоминание за час до сна.",
        },
    ],
    "meals": [
        {
            "title": "Сбалансировать приём пищи",
            "description": "Распределите калории и белок на оставшиеся приёмы — ближе к цели из профиля.",
            "action": "Добавьте следующий приём в дневник питания.",
        },
        {
            "title": "Записать обед",
            "description": "Без записи сложно видеть прогресс по калориям и БЖУ за день.",
            "action": "Откройте раздел «Питание» и сохраните блюдо.",
        },
    ],
    "state": [
        {
            "title": "Отметить самочувствие",
            "description": "Короткая отметка настроения помогает видеть связь сном, едой и активностью.",
            "action": "Заполните чек-ин настроения на главной.",
        },
    ],
}


@dataclass
class TodayGoalsSnapshot:
    water_ml: int = 0
    water_target: int = 2500
    steps: int = 0
    steps_target: int = 10_000
    burned: int = 0
    burn_target: int = 500
    sleep_hours: float = 0.0
    sleep_target: float = 8.0
    calories: int = 0
    calories_target: int = 2200
    meal_count: int = 0
    training_minutes: int = 0
    state_logged_today: bool = False


def _burn_goal(profile: UserProfile | None) -> int:
    steps = int(profile.target_steps if profile and profile.target_steps else 10_000)
    base = max(400, int(steps * 0.05))
    if profile and profile.goal == "LOSE_WEIGHT":
        base = int(base * 1.2)
    elif profile and profile.goal == "GAIN_MUSCLE":
        base = int(base * 1.05)
    return base


def collect_today_goals(db: Session, user_id: int) -> TodayGoalsSnapshot:
    today = datetime.now(timezone.utc).date()
    range_start, range_end = DailySummaryService._get_day_range(today)
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()

    water_ml = int(
        sum(
            float(h.amount_ml or 0) * (float(h.hydration_factor) if h.hydration_factor else 1.0)
            for h in db.query(HydrationRecord)
            .filter(
                HydrationRecord.user_id == user_id,
                HydrationRecord.record_time >= range_start,
                HydrationRecord.record_time < range_end,
            )
            .all()
        )
    )

    activities = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= range_start,
            ActivityRecord.start_time < range_end,
        )
        .all()
    )
    walk_steps = [int(a.steps or 0) for a in activities if (a.activity_type or "").lower() in _WALK]
    steps = max(walk_steps) if walk_steps else max((int(a.steps or 0) for a in activities), default=0)
    training = sum(
        float(a.calories_burned or 0)
        for a in activities
        if (a.activity_type or "").lower() not in _WALK
    )
    walk_kcal = next(
        (
            float(a.calories_burned or 0)
            for a in activities
            if (a.activity_type or "").lower() in _WALK and (a.calories_burned or 0) > 0
        ),
        0.0,
    )
    burned = int(training + (walk_kcal if walk_kcal > 0 else steps * 0.04))
    training_minutes = sum(
        int(a.duration_minutes or 0)
        for a in activities
        if (a.activity_type or "").lower() not in _WALK
    )

    sleep_hours = float(
        sum(
            float(s.duration_hours or 0)
            for s in db.query(SleepRecord)
            .filter(
                SleepRecord.user_id == user_id,
                SleepRecord.sleep_end >= range_start,
                SleepRecord.sleep_end < range_end,
            )
            .all()
        )
    )

    meals = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == user_id,
            MealRecord.meal_time >= range_start,
            MealRecord.meal_time < range_end,
        )
        .all()
    )
    calories = int(sum(float(m.calories or 0) for m in meals))

    state_logged = (
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= range_start,
            UserState.record_time < range_end,
        )
        .first()
        is not None
    )

    return TodayGoalsSnapshot(
        water_ml=water_ml,
        water_target=int(profile.target_water_ml if profile and profile.target_water_ml else 2500),
        steps=steps,
        steps_target=int(profile.target_steps if profile and profile.target_steps else 10_000),
        burned=burned,
        burn_target=_burn_goal(profile),
        sleep_hours=sleep_hours,
        sleep_target=float(profile.target_sleep_hours if profile and profile.target_sleep_hours else 8.0),
        calories=calories,
        calories_target=int(profile.target_daily_calories if profile and profile.target_daily_calories else 2200),
        meal_count=len(meals),
        training_minutes=training_minutes,
        state_logged_today=state_logged,
    )


def category_goal_met(category: str, goals: TodayGoalsSnapshot) -> bool:
    cat = (category or "").lower()
    if cat == "hydration":
        return goals.water_ml >= goals.water_target
    if cat == "activity":
        return (
            goals.steps >= goals.steps_target
            or goals.burned >= goals.burn_target
            or goals.training_minutes >= 30
        )
    if cat == "sleep":
        return goals.sleep_hours >= goals.sleep_target
    if cat in ("meals", "nutrition", "correlation"):
        if goals.calories_target <= 0:
            return goals.meal_count >= 2
        ratio = goals.calories / goals.calories_target
        return goals.meal_count >= 2 and 0.85 <= ratio <= 1.15
    if cat == "state":
        return goals.state_logged_today
    return False


def _gap_score(category: str, goals: TodayGoalsSnapshot) -> float:
    if category_goal_met(category, goals):
        return 0.0
    cat = (category or "").lower()
    if cat == "hydration" and goals.water_target > 0:
        return 1.0 - min(1.0, goals.water_ml / goals.water_target)
    if cat == "activity":
        step_gap = 1.0 - min(1.0, goals.steps / max(goals.steps_target, 1))
        burn_gap = 1.0 - min(1.0, goals.burned / max(goals.burn_target, 1))
        return max(step_gap, burn_gap)
    if cat == "sleep" and goals.sleep_target > 0:
        return 1.0 - min(1.0, goals.sleep_hours / goals.sleep_target)
    if cat in ("meals", "nutrition"):
        if goals.calories_target <= 0:
            return 0.5 if goals.meal_count < 2 else 0.0
        return abs(1.0 - goals.calories / goals.calories_target)
    if cat == "state":
        return 0.0 if goals.state_logged_today else 0.7
    return 0.4


def _normalize_rec(rec: Any) -> dict[str, Any]:
    if isinstance(rec, dict):
        return rec
    return {
        "category": rec.category,
        "title": rec.title,
        "description": rec.description,
        "priority": rec.priority,
        "action": getattr(rec, "action", None),
        "related_insight_type": getattr(rec, "related_insight_type", None),
        "related_insight_title": getattr(rec, "related_insight_title", None),
        "confidence": getattr(rec, "confidence", 0.7),
    }


def _fallback_for_category(category: str, user_id: int, today: date) -> dict[str, Any] | None:
    variants = _FALLBACK_TASKS.get(category)
    if not variants:
        return None
    idx = (user_id + today.toordinal()) % len(variants)
    v = variants[idx]
    return {
        "category": category,
        "title": v["title"],
        "description": v["description"],
        "priority": "medium",
        "action": v.get("action"),
        "related_insight_type": "daily_fallback",
        "related_insight_title": None,
        "confidence": 0.6,
    }


def select_personalized_recommendations(
    db: Session,
    user_id: int,
    limit: int = 5,
) -> list[dict[str, Any]]:
    """Подбор задач: персональные факты + ротация, без категорий где цель уже закрыта."""
    today = datetime.now(timezone.utc).date()
    goals = collect_today_goals(db, user_id)

    personal = PersonalizedAdvisor.generate_recommendations(db, user_id, period_days=14)
    engine = [_normalize_rec(r) for r in RecommendationEngine.generate_recommendations(db, user_id)]

    seen_titles: set[str] = set()
    candidates: list[dict[str, Any]] = []

    for rec in personal + engine:
        title = (rec.get("title") or "").strip()
        cat = rec.get("category") or "correlation"
        if not title or title in seen_titles:
            continue
        if category_goal_met(cat, goals):
            continue
        seen_titles.add(title)
        rec = dict(rec)
        rec["gap_score"] = _gap_score(cat, goals)
        candidates.append(rec)

    # По одной задаче на «отстающую» категорию, затем добор по приоритету
    by_cat: dict[str, list[dict[str, Any]]] = {}
    for c in candidates:
        by_cat.setdefault(c.get("category") or "correlation", []).append(c)

    rng = random.Random(user_id * 10_000 + today.toordinal())
    selected: list[dict[str, Any]] = []
    open_categories = sorted(
        by_cat.keys(),
        key=lambda cat: -max(item["gap_score"] for item in by_cat[cat]),
    )
    for cat in open_categories:
        pool = by_cat[cat]
        pool.sort(
            key=lambda r: (
                -r.get("gap_score", 0),
                -_PRIORITY_SCORE.get(r.get("priority", "medium"), 1),
                -(r.get("confidence") or 0),
            ),
        )
        rng.shuffle(pool)
        selected.append(pool[0])
        if len(selected) >= limit:
            break

    if len(selected) < limit:
        rest = [c for c in candidates if c not in selected]
        rng.shuffle(rest)
        rest.sort(
            key=lambda r: (
                -r.get("gap_score", 0),
                -_PRIORITY_SCORE.get(r.get("priority", "medium"), 1),
            ),
        )
        for c in rest:
            if c not in selected:
                selected.append(c)
            if len(selected) >= limit:
                break

    # Запасные карточки для категорий без рекомендаций
    for cat in ("hydration", "activity", "sleep", "meals", "state"):
        if len(selected) >= limit:
            break
        if category_goal_met(cat, goals):
            continue
        if any(s.get("category") == cat for s in selected):
            continue
        fb = _fallback_for_category(cat, user_id, today)
        if fb and fb["title"] not in seen_titles:
            fb["gap_score"] = _gap_score(cat, goals)
            selected.append(fb)
            seen_titles.add(fb["title"])

    return selected[:limit]


def sync_action_plan_completion(db: Session, user_id: int) -> int:
    """Отмечает pending/in_progress пункты выполненными, если цель категории достигнута."""
    goals = collect_today_goals(db, user_id)
    items = (
        db.query(ActionPlan)
        .filter(
            ActionPlan.user_id == user_id,
            ActionPlan.status.in_(("pending", "in_progress")),
        )
        .all()
    )
    updated = 0
    for item in items:
        if category_goal_met(item.category, goals):
            item.status = "done"
            updated += 1
    if updated:
        db.commit()
    return updated


def _plan_created_date(created_at: datetime | None) -> date | None:
    if created_at is None:
        return None
    if created_at.tzinfo is not None:
        return created_at.astimezone(timezone.utc).date()
    return created_at.date()


def ensure_daily_action_plan(db: Session, user_id: int, limit: int = 5) -> None:
    """
    Новый день — новый персональный план; в тот же день — только синхронизация статусов.
    """
    from app.services.action_plan_builder import ActionPlanBuilder

    today = datetime.now(timezone.utc).date()
    items = ActionPlanBuilder.get_for_user(db=db, user_id=user_id)

    sync_action_plan_completion(db, user_id)

    if not items:
        ActionPlanBuilder.generate_for_user(
            db=db,
            user_id=user_id,
            limit=limit,
            replace_existing=True,
            use_personalized=True,
        )
        sync_action_plan_completion(db, user_id)
        return

    newest = max(items, key=lambda i: i.created_at or datetime.min.replace(tzinfo=timezone.utc))
    created = _plan_created_date(newest.created_at)
    if created is None or created < today:
        ActionPlanBuilder.generate_for_user(
            db=db,
            user_id=user_id,
            limit=limit,
            replace_existing=True,
            use_personalized=True,
        )
        sync_action_plan_completion(db, user_id)
