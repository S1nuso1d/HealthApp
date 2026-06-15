"""Полный контекст дневника пользователя для AI-чата."""

from __future__ import annotations

from datetime import date, datetime, timedelta, timezone

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.daily_health_summary import DailyHealthSummary
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.services.action_plan_sync_service import collect_today_goals
from app.services.analytics.user_trends_service import compute_user_trends
from app.services.health_metrics import normalize_mood_value
from app.services.health_score_service import compute_today_scores
from app.services.personalized_advisor import PersonalizedAdvisor
from app.services.recommendation_orchestrator import build_merged_recommendation_items


def _fmt_dt(dt: datetime | None) -> str:
    if dt is None:
        return "—"
    if dt.tzinfo is None:
        dt = dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc).strftime("%d.%m.%Y %H:%M")


def _day_range(target: date) -> tuple[datetime, datetime]:
    start = datetime.combine(target, datetime.min.time()).replace(tzinfo=timezone.utc)
    return start, start + timedelta(days=1)


def build_user_health_context_text(
    db: Session,
    user_id: int,
    period_days: int = 14,
) -> str:
    period_days = max(3, min(period_days, 30))
    end_date = date.today()
    start_date = end_date - timedelta(days=period_days - 1)

    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    goals = collect_today_goals(db, user_id)
    scores = compute_today_scores(db, user_id)
    trends = compute_user_trends(db, user_id, days=period_days)

    lines: list[str] = [
        "=== ПРОФИЛЬ И ЦЕЛИ ===",
    ]
    if profile:
        lines.append(
            f"Возраст: {profile.age or '—'}, пол: {profile.sex or '—'}, "
            f"рост: {profile.height_cm or '—'} см, вес: {profile.weight_kg or '—'} кг"
        )
        lines.append(
            f"Цель приложения: {profile.goal or '—'}, активность: {profile.activity_level or '—'}"
        )
        lines.append(
            f"Цели на день: сон {profile.target_sleep_hours or 8} ч, "
            f"вода {profile.target_water_ml or 2500} мл, "
            f"шаги {profile.target_steps or 10000}, "
            f"калории {profile.target_daily_calories or 2200} ккал"
        )
        if profile.has_allergies and profile.allergies_text:
            lines.append(f"Аллергии/ограничения: {profile.allergies_text}")
        elif profile.has_allergies:
            lines.append("Аллергии/ограничения: указаны в профиле, уточните список продуктов")
        is_veg = profile.is_vegetarian or (
            profile.allergies_text
            and any(
                token in profile.allergies_text.lower()
                for token in ("вегетариан", "vegetarian", "vegan", "веган", "без мяса")
            )
        )
        if is_veg:
            lines.append(
                "Питание: СТРОГО ВЕГЕТАРИАНСКОЕ — без мяса, птицы, рыбы, морепродуктов и рыбных соусов"
            )
    else:
        lines.append("Профиль не заполнен")

    lines.extend(
        [
            "",
            "=== СЕГОДНЯ (факт / цель) ===",
            f"Дата: {end_date.isoformat()}",
            f"Сон: {goals.sleep_hours:.1f} / {goals.sleep_target:.1f} ч",
            f"Вода: {goals.water_ml} / {goals.water_target} мл",
            f"Шаги: {goals.steps} / {goals.steps_target}",
            f"Калории еды: {goals.calories} / {goals.calories_target} ккал",
            f"Сожжено: {goals.burned} / {goals.burn_target} ккал",
            f"Тренировки (мин): {goals.training_minutes}",
            f"Приёмов пищи: {goals.meal_count}",
            f"Отметка настроения сегодня: {'да' if goals.state_logged_today else 'нет'}",
            "",
            "=== ИНДЕКСЫ ЗА СЕГОДНЯ (0–100) ===",
            f"Общий: {scores['health_score']}, сон: {scores['sleep_score']}, "
            f"вода: {scores['hydration_score']}, активность: {scores['activity_score']}, "
            f"питание: {scores['nutrition_score']}, состояние: {scores['state_score']}",
        ]
    )

    if trends.days_with_data > 0:
        lines.extend(
            [
                "",
                f"=== ТРЕНДЫ ЗА {period_days} ДН. ===",
                f"Дней с записями: {trends.days_with_data}, дней с выполненными целями: {trends.goals_met_days}",
            ]
        )
        if trends.avg_sleep_hours is not None:
            lines.append(f"Средний сон: {trends.avg_sleep_hours} ч")
        if trends.avg_water_ml is not None:
            lines.append(f"Средняя вода: {int(trends.avg_water_ml)} мл/день")
        if trends.avg_steps is not None:
            lines.append(f"Средние шаги: {int(trends.avg_steps)}/день")
        if trends.sleep_delta_vs_prev is not None:
            lines.append(f"Динамика сна (2-я половина периода): {trends.sleep_delta_vs_prev:+.1f} ч")
        if trends.water_delta_vs_prev is not None:
            lines.append(f"Динамика воды: {trends.water_delta_vs_prev:+.0f} мл")
        if trends.steps_delta_vs_prev is not None:
            lines.append(f"Динамика шагов: {trends.steps_delta_vs_prev:+.0f}")

    summaries = (
        db.query(DailyHealthSummary)
        .filter(
            DailyHealthSummary.user_id == user_id,
            DailyHealthSummary.summary_date >= start_date,
            DailyHealthSummary.summary_date <= end_date,
        )
        .order_by(DailyHealthSummary.summary_date.desc())
        .limit(7)
        .all()
    )
    if summaries:
        lines.append("")
        lines.append("=== ПОСЛЕДНИЕ ДНИ (сводки) ===")
        for s in summaries:
            lines.append(
                f"{s.summary_date}: сон {s.total_sleep_hours or 0:.1f} ч, "
                f"вода {s.total_water_ml or 0} мл, шаги {s.total_steps or 0}, "
                f"ккал {s.total_calories or 0:.0f}, кофеин {s.total_caffeine_mg or 0:.0f} мг"
            )

    period_start = datetime.combine(start_date, datetime.min.time()).replace(tzinfo=timezone.utc)
    period_end = datetime.combine(end_date + timedelta(days=1), datetime.min.time()).replace(
        tzinfo=timezone.utc
    )

    sleeps = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == user_id,
            SleepRecord.sleep_end >= period_start,
            SleepRecord.sleep_end < period_end,
        )
        .order_by(SleepRecord.sleep_end.desc())
        .limit(8)
        .all()
    )
    if sleeps:
        lines.append("")
        lines.append("=== СОН (последние записи) ===")
        for s in sleeps:
            lines.append(
                f"{_fmt_dt(s.sleep_end)}: {s.duration_hours or 0:.1f} ч, "
                f"качество {s.quality_score or '—'}/10"
                + (f", заметка: {s.notes}" if s.notes else "")
            )

    meals = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == user_id,
            MealRecord.meal_time >= period_start,
            MealRecord.meal_time < period_end,
        )
        .order_by(MealRecord.meal_time.desc())
        .limit(12)
        .all()
    )
    if meals:
        lines.append("")
        lines.append("=== ПИТАНИЕ (последние приёмы) ===")
        for m in meals:
            late = " поздний" if getattr(m, "is_late_meal", None) in (1, True) else ""
            lines.append(
                f"{_fmt_dt(m.meal_time)} {m.meal_type or 'еда'}{late}: "
                f"{m.name or '—'}, {m.calories or 0:.0f} ккал, "
                f"Б/Ж/У {m.protein_g or 0:.0f}/{m.fat_g or 0:.0f}/{m.carbs_g or 0:.0f} г"
                + (f", кофеин {m.caffeine_mg:.0f} мг" if (m.caffeine_mg or 0) > 0 else "")
            )

    day_start, day_end = _day_range(end_date)
    hydrations = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.user_id == user_id,
            HydrationRecord.record_time >= period_start,
            HydrationRecord.record_time < period_end,
        )
        .order_by(HydrationRecord.record_time.desc())
        .limit(15)
        .all()
    )
    if hydrations:
        today_water = sum(
            float(h.amount_ml or 0) * (float(h.hydration_factor) if h.hydration_factor else 1.0)
            for h in hydrations
            if day_start <= (h.record_time.replace(tzinfo=timezone.utc) if h.record_time.tzinfo is None else h.record_time) < day_end
        )
        lines.append("")
        lines.append(f"=== ВОДА: записей за период {len(hydrations)}, сегодня ~{int(today_water)} мл ===")
        for h in hydrations[:6]:
            lines.append(f"{_fmt_dt(h.record_time)}: {h.amount_ml} мл")

    activities = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= period_start,
            ActivityRecord.start_time < period_end,
        )
        .order_by(ActivityRecord.start_time.desc())
        .limit(10)
        .all()
    )
    if activities:
        lines.append("")
        lines.append("=== АКТИВНОСТЬ (последние) ===")
        for a in activities:
            lines.append(
                f"{_fmt_dt(a.start_time)} {a.activity_type or 'активность'}: "
                f"{a.duration_minutes or 0} мин, шаги {a.steps or 0}, "
                f"сожжено {a.calories_burned or 0:.0f} ккал"
            )

    states = (
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= period_start,
            UserState.record_time < period_end,
        )
        .order_by(UserState.record_time.desc())
        .limit(8)
        .all()
    )
    if states:
        lines.append("")
        lines.append("=== САМОЧУВСТВИЕ (отметки) ===")
        for st in states:
            mood_txt = f"{st.mood}/5" if st.mood is not None else "—"
            if st.mood is not None:
                mood_txt += f" (норм. {normalize_mood_value(float(st.mood)):.0f}/10)"
            lines.append(
                f"{_fmt_dt(st.record_time)}: настроение {mood_txt}, "
                f"энергия {st.energy or '—'}/10, стресс {st.stress or '—'}/10"
                + (f", фокус {st.focus}/10" if st.focus is not None else "")
                + (f". {st.notes}" if st.notes else "")
            )

    personal = PersonalizedAdvisor.generate_recommendations(db, user_id, period_days=period_days)[:6]
    merged = build_merged_recommendation_items(db, user_id, period_days)[:6]
    rec_titles = {r["title"] for r in personal}
    for r in merged:
        if r.title not in rec_titles and len(personal) < 8:
            personal.append(
                {
                    "title": r.title,
                    "description": r.description,
                    "action": r.action,
                    "category": r.category,
                    "priority": r.priority,
                }
            )

    if personal:
        lines.append("")
        lines.append("=== ПЕРСОНАЛЬНЫЕ РЕКОМЕНДАЦИИ СИСТЕМЫ ===")
        for r in personal[:8]:
            action = r.get("action") or ""
            lines.append(f"- [{r.get('category', '')}] {r.get('title', '')}: {r.get('description', '')}")
            if action:
                lines.append(f"  Действие: {action}")

    return "\n".join(lines)
