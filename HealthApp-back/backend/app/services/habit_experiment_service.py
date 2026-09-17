"""Семидневный эксперимент: убрать один фактор и сравнить сон.

Это не A/B в лабораторном смысле — слишком мало данных. Но человеку нужна
петля: «попробовал неделю без кофе после 15:00 — сон стал длиннее на 40 минут».
"""

from __future__ import annotations

from datetime import date, datetime, timedelta, timezone

from sqlalchemy.orm import Session

from app.models.habit_experiment import HabitExperiment
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.services.ai.time_context import get_local_now

EXPERIMENT_DAYS = 7

PRESETS: dict[str, dict[str, str]] = {
    "late_meal_sleep_impact": {
        "title": "Ужин до 20:00",
        "action": "7 дней без еды после 20:00",
        "metric": "sleep_hours",
    },
    "late_caffeine_sleep_impact": {
        "title": "Без кофеина после 15:00",
        "action": "7 дней без кофеина после 15:00",
        "metric": "sleep_hours",
    },
    "evening_high_activity_sleep_impact": {
        "title": "Без тяжёлых тренировок вечером",
        "action": "7 дней: интенсивность только до 18:00",
        "metric": "sleep_hours",
    },
    "late_drink_sleep_impact": {
        "title": "Вода до 19:00",
        "action": "7 дней основной объём воды до 19:00",
        "metric": "sleep_hours",
    },
    "late_meal_and_evening_workout_sleep": {
        "title": "Не совмещать поздний ужин и тренировку",
        "action": "7 дней: либо ранняя еда, либо дневная нагрузка",
        "metric": "sleep_hours",
    },
    "low_hydration_low_energy": {
        "title": "Вода в первой половине дня",
        "action": "7 дней: 500 мл до полудня",
        "metric": "energy",
    },
}


def _utc_now() -> datetime:
    return datetime.now(timezone.utc)


def _to_dict(item: HabitExperiment) -> dict:
    elapsed = _days_elapsed(item)
    return {
        "id": item.id,
        "factor_id": item.factor_id,
        "title": item.title,
        "action": item.action,
        "metric": item.metric,
        "status": item.status,
        "started_on": item.started_on.isoformat() if item.started_on else None,
        "ends_on": item.ends_on.isoformat() if item.ends_on else None,
        "baseline_value": item.baseline_value,
        "result_value": item.result_value,
        "result_summary": item.result_summary,
        "days_total": EXPERIMENT_DAYS,
        "days_elapsed": elapsed,
        "days_left": max(0, EXPERIMENT_DAYS - elapsed),
    }


def _days_elapsed(item: HabitExperiment) -> int:
    if item.started_on is None:
        return 0
    today = get_local_now().date()
    return max(0, min(EXPERIMENT_DAYS, (today - item.started_on).days))


def _avg_sleep_hours(db: Session, user_id: int, start: date, end: date) -> float | None:
    start_dt = datetime.combine(start, datetime.min.time()).replace(tzinfo=timezone.utc)
    end_dt = datetime.combine(end + timedelta(days=1), datetime.min.time()).replace(tzinfo=timezone.utc)
    rows = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == user_id,
            SleepRecord.sleep_end >= start_dt,
            SleepRecord.sleep_end < end_dt,
        )
        .all()
    )
    hours = [float(s.duration_hours) for s in rows if s.duration_hours]
    if len(hours) < 2:
        return None
    return round(sum(hours) / len(hours), 2)


def _avg_energy(db: Session, user_id: int, start: date, end: date) -> float | None:
    start_dt = datetime.combine(start, datetime.min.time()).replace(tzinfo=timezone.utc)
    end_dt = datetime.combine(end + timedelta(days=1), datetime.min.time()).replace(tzinfo=timezone.utc)
    rows = (
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= start_dt,
            UserState.record_time < end_dt,
            UserState.energy.isnot(None),
        )
        .all()
    )
    values = [float(s.energy) for s in rows if s.energy is not None]
    if len(values) < 2:
        return None
    return round(sum(values) / len(values), 2)


def _metric_average(db: Session, user_id: int, metric: str, start: date, end: date) -> float | None:
    if metric == "energy":
        return _avg_energy(db, user_id, start, end)
    return _avg_sleep_hours(db, user_id, start, end)


def _maybe_complete(db: Session, item: HabitExperiment) -> HabitExperiment:
    if item.status != "active":
        return item
    today = get_local_now().date()
    if item.ends_on is None or today < item.ends_on:
        return item

    result = _metric_average(db, item.user_id, item.metric, item.started_on, item.ends_on)
    item.result_value = result
    item.status = "completed"
    baseline = item.baseline_value
    unit = "ч сна" if item.metric == "sleep_hours" else "балла энергии"
    if result is None or baseline is None:
        item.result_summary = (
            "Неделя прошла, но ночей или отметок мало, чтобы сравнить. "
            "Повторите эксперимент, когда дневник будет плотнее."
        )
    else:
        delta = result - baseline
        if abs(delta) < 0.15:
            item.result_summary = (
                f"Почти без изменения: было {baseline:.1f} {unit}, стало {result:.1f}."
            )
        elif delta > 0:
            item.result_summary = (
                f"Сработало: {baseline:.1f} → {result:.1f} {unit} "
                f"(+{delta:.1f}). Имеет смысл оставить правило."
            )
        else:
            item.result_summary = (
                f"Стало хуже: {baseline:.1f} → {result:.1f} {unit} "
                f"({delta:.1f}). Правило можно отменить и попробовать другой рычаг."
            )
    db.commit()
    db.refresh(item)
    return item


def list_experiments(db: Session, user_id: int) -> list[dict]:
    items = (
        db.query(HabitExperiment)
        .filter(HabitExperiment.user_id == user_id)
        .order_by(HabitExperiment.created_at.desc())
        .limit(10)
        .all()
    )
    return [_to_dict(_maybe_complete(db, item)) for item in items]


def get_active_experiment(db: Session, user_id: int) -> dict | None:
    items = (
        db.query(HabitExperiment)
        .filter(HabitExperiment.user_id == user_id)
        .order_by(HabitExperiment.created_at.desc())
        .limit(5)
        .all()
    )
    for item in items:
        fresh = _maybe_complete(db, item)
        if fresh.status == "active":
            return _to_dict(fresh)
        if fresh.status == "completed" and fresh.ends_on:
            today = get_local_now().date()
            if (today - fresh.ends_on).days <= 2:
                return _to_dict(fresh)
    return None


def start_experiment(
    db: Session,
    user_id: int,
    factor_id: str,
    title: str | None = None,
    action: str | None = None,
) -> dict:
    active = (
        db.query(HabitExperiment)
        .filter(HabitExperiment.user_id == user_id, HabitExperiment.status == "active")
        .first()
    )
    if active:
        active.status = "cancelled"
        db.flush()

    preset = PRESETS.get(factor_id, {
        "title": title or "Недельный эксперимент",
        "action": action or "7 дней без выбранного фактора",
        "metric": "sleep_hours",
    })
    today = get_local_now().date()
    metric = preset["metric"]
    baseline_start = today - timedelta(days=EXPERIMENT_DAYS)
    baseline = _metric_average(db, user_id, metric, baseline_start, today - timedelta(days=1))

    item = HabitExperiment(
        user_id=user_id,
        factor_id=factor_id,
        title=title or preset["title"],
        action=action or preset["action"],
        metric=metric,
        status="active",
        started_on=today,
        ends_on=today + timedelta(days=EXPERIMENT_DAYS),
        baseline_value=baseline,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return _to_dict(item)


def cancel_experiment(db: Session, user_id: int, experiment_id: int) -> dict:
    item = (
        db.query(HabitExperiment)
        .filter(HabitExperiment.id == experiment_id, HabitExperiment.user_id == user_id)
        .first()
    )
    if item is None:
        return {}
    if item.status == "active":
        item.status = "cancelled"
        db.commit()
        db.refresh(item)
    return _to_dict(item)
