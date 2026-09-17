"""Вечерний риск сна — один рычаг, а не отчёт.

Считается по записям за сегодня: поздняя еда, кофеин, интенсивная тренировка,
позднее питьё. Цифры берутся из дневника, а не из общих правил учебника.
"""

from __future__ import annotations

from datetime import datetime, timedelta

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.services.ai.time_context import get_local_now
from app.services.correlation_analyzer import normalize_bool


def _aware(dt: datetime, tz) -> datetime:
    if dt.tzinfo is None:
        return dt.replace(tzinfo=tz)
    return dt.astimezone(tz)


def build_tonight_risk(db: Session, user_id: int) -> dict:
    now = get_local_now()
    tz = now.tzinfo
    today = now.date()
    # SQLite часто хранит timestamp без надёжного смещения, поэтому берём
    # широкий хвост и оставляем только записи сегодняшнего локального дня.
    lookback = now - timedelta(hours=36)
    lookahead = now + timedelta(hours=24)

    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    sleep_target = float(profile.target_sleep_hours or 8) if profile else 8.0

    meals = [
        meal
        for meal in (
            db.query(MealRecord)
            .filter(
                MealRecord.user_id == user_id,
                MealRecord.meal_time >= lookback,
                MealRecord.meal_time <= lookahead,
            )
            .order_by(MealRecord.meal_time.asc())
            .all()
        )
        if _aware(meal.meal_time, tz).date() == today
    ]
    activities = [
        activity
        for activity in (
            db.query(ActivityRecord)
            .filter(
                ActivityRecord.user_id == user_id,
                ActivityRecord.start_time >= lookback,
                ActivityRecord.start_time <= lookahead,
            )
            .order_by(ActivityRecord.start_time.asc())
            .all()
        )
        if _aware(activity.start_time, tz).date() == today
    ]
    drinks = [
        drink
        for drink in (
            db.query(HydrationRecord)
            .filter(
                HydrationRecord.user_id == user_id,
                HydrationRecord.record_time >= lookback,
                HydrationRecord.record_time <= lookahead,
            )
            .order_by(HydrationRecord.record_time.asc())
            .all()
        )
        if _aware(drink.record_time, tz).date() == today
    ]

    levers: list[dict] = []
    score = 15

    late_meals = []
    late_caffeine = []
    for meal in meals:
        mt = _aware(meal.meal_time, tz)
        is_late = normalize_bool(getattr(meal, "is_late_meal", False)) or mt.hour >= 20
        if is_late:
            late_meals.append(meal)
        if float(meal.caffeine_mg or 0) >= 30 and mt.hour >= 16:
            late_caffeine.append(meal)

    if late_meals:
        last = max(late_meals, key=lambda m: _aware(m.meal_time, tz))
        t = _aware(last.meal_time, tz).strftime("%H:%M")
        name = last.name or "еда"
        score += 28
        levers.append(
            {
                "id": "late_meal_sleep_impact",
                "title": f"Ужин в {t}",
                "why": f"«{name}» в {t} — поздно относительно типичного засыпания.",
                "action": "Завтра завершите еду до 20:00.",
            }
        )
    if late_caffeine:
        last = max(late_caffeine, key=lambda m: _aware(m.meal_time, tz))
        t = _aware(last.meal_time, tz).strftime("%H:%M")
        mg = int(last.caffeine_mg or 0)
        score += 28
        levers.append(
            {
                "id": "late_caffeine_sleep_impact",
                "title": f"Кофеин в {t}",
                "why": f"{mg} мг кофеина после 16:00. Для многих это сдвигает засыпание.",
                "action": "7 дней без кофеина после 15:00.",
            }
        )

    evening_hard = [
        a
        for a in activities
        if (a.intensity or "").lower() == "high"
        and _aware(a.start_time, tz).hour >= 18
    ]
    if evening_hard:
        last = max(evening_hard, key=lambda a: _aware(a.start_time, tz))
        t = _aware(last.start_time, tz).strftime("%H:%M")
        mins = int(last.duration_minutes or 0)
        score += 22
        levers.append(
            {
                "id": "evening_high_activity_sleep_impact",
                "title": f"Интенсивная нагрузка в {t}",
                "why": f"{mins} мин высокой интенсивности вечером могут мешать расслаблению.",
                "action": "Перенесите тяжёлую тренировку на день, вечером — прогулка.",
            }
        )

    late_drinks = [
        d
        for d in drinks
        if normalize_bool(getattr(d, "is_late_drink", False))
        or _aware(d.record_time, tz).hour >= 21
    ]
    if late_drinks:
        last = max(late_drinks, key=lambda d: _aware(d.record_time, tz))
        t = _aware(last.record_time, tz).strftime("%H:%M")
        ml = int(last.amount_ml or 0)
        score += 12
        levers.append(
            {
                "id": "late_drink_sleep_impact",
                "title": f"Напиток в {t}",
                "why": f"{ml} мл поздно вечером — частые пробуждения из‑за туалета.",
                "action": "Основной объём воды — до 19:00, вечером только маленькими глотками.",
            }
        )

    score = max(0, min(100, score if levers else 18))
    if not levers:
        level = "low"
        headline = "По записям за сегодня явных угроз для сна нет"
        primary = "Если ещё не легли — приглушите свет и уберите экран."
    elif score >= 60:
        level = "high"
        headline = "Сегодня риск короткого сна высокий"
        primary = levers[0]["action"]
    else:
        level = "medium"
        headline = "Есть факторы, которые могут укоротить сон"
        primary = levers[0]["action"]

    suggested = None
    if levers:
        top = levers[0]
        suggested = {
            "factor_id": top["id"],
            "title": top["title"],
            "action": top["action"],
            "metric": "sleep_hours",
        }

    return {
        "generated_at": now.isoformat(),
        "local_hour": now.hour,
        "is_evening": now.hour >= 18 or now.hour < 5,
        "sleep_target_hours": sleep_target,
        "risk_level": level,
        "risk_score": score,
        "headline": headline,
        "primary_action": primary,
        "levers": levers,
        "suggested_experiment": suggested,
    }
