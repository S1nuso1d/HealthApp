"""Циркадный профиль: обычное время сна и связь ужина со сном.

Это не медицинский прогноз. Нужно, чтобы вовремя сказать:
«обычно вы засыпаете в 22:10 — лучше не начинать тяжёлую тренировку сейчас».
"""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from statistics import median

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.services.ai.time_context import get_local_now

MIN_NIGHTS = 4
WORKOUT_WINDOW_BEFORE_MIN = 120
WORKOUT_WINDOW_AFTER_MIN = 30
NUDGE_BEFORE_MIN = 40


def _aware(dt: datetime, tz) -> datetime:
    if dt.tzinfo is None:
        return dt.replace(tzinfo=tz)
    return dt.astimezone(tz)


def _clock_minutes(local: datetime) -> int:
    minutes = local.hour * 60 + local.minute
    # Отбой после полуночи относится к «прошлому вечеру», иначе медиана плывёт.
    if local.hour < 5:
        minutes += 24 * 60
    return minutes


def _label_minutes(minutes: int) -> str:
    wrapped = minutes % (24 * 60)
    return f"{wrapped // 60:02d}:{wrapped % 60:02d}"


def usual_bedtime_minutes(db: Session, user_id: int, nights: int = 14) -> dict:
    now = get_local_now()
    tz = now.tzinfo
    since = now - timedelta(days=nights + 1)
    sleeps = (
        db.query(SleepRecord)
        .filter(SleepRecord.user_id == user_id, SleepRecord.sleep_start >= since)
        .order_by(SleepRecord.sleep_start.desc())
        .limit(nights)
        .all()
    )
    values = [_clock_minutes(_aware(item.sleep_start, tz)) for item in sleeps if item.sleep_start]
    if len(values) < MIN_NIGHTS:
        return {
            "sample_nights": len(values),
            "usual_minutes": None,
            "usual_label": None,
            "consistency_minutes": None,
        }
    usual = int(round(median(values)))
    spread = int(round(median(abs(v - usual) for v in values)))
    return {
        "sample_nights": len(values),
        "usual_minutes": usual,
        "usual_label": _label_minutes(usual),
        "consistency_minutes": spread,
    }


def hours_before_sleep_curve(db: Session, user_id: int, nights: int = 14) -> list[dict]:
    """Средний сон в зависимости от того, за сколько часов до отбоя был ужин."""
    now = get_local_now()
    tz = now.tzinfo
    since = now - timedelta(days=nights + 1)
    sleeps = (
        db.query(SleepRecord)
        .filter(SleepRecord.user_id == user_id, SleepRecord.sleep_start >= since)
        .order_by(SleepRecord.sleep_start.desc())
        .limit(nights)
        .all()
    )
    meals = (
        db.query(MealRecord)
        .filter(MealRecord.user_id == user_id, MealRecord.meal_time >= since - timedelta(hours=8))
        .order_by(MealRecord.meal_time.asc())
        .all()
    )
    buckets: dict[str, list[float]] = {
        "0_2": [],
        "2_3": [],
        "3_4": [],
        "4_plus": [],
    }
    labels = {
        "0_2": "меньше 2 ч до сна",
        "2_3": "2–3 ч до сна",
        "3_4": "3–4 ч до сна",
        "4_plus": "больше 4 ч до сна",
    }
    for sleep in sleeps:
        if not sleep.sleep_start or not sleep.duration_hours:
            continue
        start = _aware(sleep.sleep_start, tz)
        last_meal = None
        for meal in meals:
            meal_time = _aware(meal.meal_time, tz)
            if meal_time <= start:
                last_meal = meal_time
            else:
                break
        if last_meal is None:
            continue
        hours = (start - last_meal).total_seconds() / 3600
        if hours < 0 or hours > 12:
            continue
        key = "0_2" if hours < 2 else "2_3" if hours < 3 else "3_4" if hours < 4 else "4_plus"
        buckets[key].append(float(sleep.duration_hours))

    curve = []
    for key, values in buckets.items():
        if len(values) < 2:
            continue
        curve.append(
            {
                "bucket": key,
                "label": labels[key],
                "nights": len(values),
                "sleep_hours": round(sum(values) / len(values), 1),
            }
        )
    return curve


def workout_near_bedtime_warning(
    start: datetime,
    usual_minutes: int | None,
    intensity: str | None = None,
) -> str | None:
    if usual_minutes is None:
        return None
    if (intensity or "").lower() == "low":
        return None
    tz = get_local_now().tzinfo
    local = _aware(start, tz)
    start_minutes = local.hour * 60 + local.minute
    if local.hour < 5:
        start_minutes += 24 * 60
    delta = usual_minutes - start_minutes
    if delta < -WORKOUT_WINDOW_AFTER_MIN or delta > WORKOUT_WINDOW_BEFORE_MIN:
        return None
    label = _label_minutes(usual_minutes)
    return (
        f"Обычно вы засыпаете около {label}. Тренировка в этом окне часто мешает "
        "расслабиться — лучше перенести нагрузку на день, вечером оставить прогулку."
    )


def bedtime_nudge(usual_minutes: int | None, sample_nights: int) -> str | None:
    if usual_minutes is None or sample_nights < MIN_NIGHTS:
        return None
    now = get_local_now()
    now_minutes = now.hour * 60 + now.minute
    if now.hour < 5:
        now_minutes += 24 * 60
    delta = usual_minutes - now_minutes
    if delta < 0 or delta > NUDGE_BEFORE_MIN:
        return None
    label = _label_minutes(usual_minutes)
    return (
        f"Обычно вы ложитесь около {label}. Стабильный отбой помогает сну лучше, "
        "чем разово «отоспаться» в выходные. Приглушите свет и уберите экран."
    )


def build_circadian(db: Session, user_id: int, nights: int = 14) -> dict:
    bedtime = usual_bedtime_minutes(db, user_id, nights=nights)
    usual = bedtime["usual_minutes"]
    nudge = bedtime_nudge(usual, bedtime["sample_nights"])
    now = get_local_now()
    recent_workout_warning = None
    if usual is not None:
        lookback = now - timedelta(hours=4)
        recent = (
            db.query(ActivityRecord)
            .filter(
                ActivityRecord.user_id == user_id,
                ActivityRecord.start_time >= lookback,
            )
            .order_by(ActivityRecord.start_time.desc())
            .limit(3)
            .all()
        )
        for item in recent:
            recent_workout_warning = workout_near_bedtime_warning(
                item.start_time,
                usual,
                item.intensity,
            )
            if recent_workout_warning:
                break

    notify_minutes = None
    if usual is not None:
        notify_minutes = (usual - 30) % (24 * 60)

    return {
        "sample_nights": bedtime["sample_nights"],
        "usual_bedtime": bedtime["usual_label"],
        "usual_bedtime_hour": None if usual is None else (usual % (24 * 60)) // 60,
        "usual_bedtime_minute": None if usual is None else (usual % (24 * 60)) % 60,
        "consistency_minutes": bedtime["consistency_minutes"],
        "notify_hour": None if notify_minutes is None else notify_minutes // 60,
        "notify_minute": None if notify_minutes is None else notify_minutes % 60,
        "bedtime_nudge": nudge,
        "recent_workout_warning": recent_workout_warning,
        "hours_before_sleep": hours_before_sleep_curve(db, user_id, nights=nights),
        "generated_at": now.isoformat(),
    }
