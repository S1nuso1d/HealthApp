"""Проверка и выдача достижений по данным дневника."""

from __future__ import annotations

from datetime import datetime, timezone
from math import isfinite
from typing import Any

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.gamification import UserAchievement as UA
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.services.analytics.daily_summary_service import DailySummaryService

WALK_TYPES = {"walk", "walking", "ходьба"}
RUN_TYPES = {"run", "running", "jogging", "бег", "пробежка"}


ACHIEVEMENT_DEFS: dict[str, dict[str, Any]] = {
    "steps_5k": {
        "title": "5 000 шагов",
        "description": "В один день прошли 5 000 шагов — отличный ритм.",
        "icon_key": "steps",
        "points": 15,
        "kind": "daily",
        "target": 5_000,
        "unit": "шагов",
    },
    "steps_10k": {
        "title": "10 000 шагов",
        "description": "Дневная цель по шагам достигнута.",
        "icon_key": "steps",
        "points": 30,
        "kind": "daily",
        "target": 10_000,
        "unit": "шагов",
    },
    "water_goal": {
        "title": "Норма воды",
        "description": "Выпили целевой объём воды за день.",
        "icon_key": "water",
        "points": 20,
        "kind": "daily",
        "target": 2_500,
        "unit": "мл",
    },
    "sleep_8h": {
        "title": "8 часов сна",
        "description": "Полноценная ночь восстановления.",
        "icon_key": "sleep",
        "points": 25,
        "kind": "daily",
        "target": 8,
        "unit": "ч",
    },
    "burn_goal": {
        "title": "Цель по калориям",
        "description": "Сожгли запланированный объём энергии за день.",
        "icon_key": "fire",
        "points": 25,
        "kind": "daily",
        "target": 450,
        "unit": "ккал",
    },
    "first_workout": {
        "title": "Первая тренировка",
        "description": "Записали тренировку в дневник активности.",
        "icon_key": "workout",
        "points": 10,
        "kind": "daily",
        "target": 1,
        "unit": "тренировка",
    },
    "run_total_10k": {
        "title": "10 км бега суммарно",
        "description": "Накопите 10 км пробежек за всё время.",
        "icon_key": "run",
        "points": 40,
        "kind": "journey",
        "target": 10,
        "unit": "км",
    },
    "run_total_50k": {
        "title": "50 км бега суммарно",
        "description": "Большая дистанция складывается из регулярных выходов.",
        "icon_key": "run",
        "points": 90,
        "kind": "journey",
        "target": 50,
        "unit": "км",
    },
    "workouts_10": {
        "title": "10 тренировок",
        "description": "Запишите 10 тренировок длительностью от 10 минут.",
        "icon_key": "workout",
        "points": 50,
        "kind": "journey",
        "target": 10,
        "unit": "тренировок",
    },
    "active_minutes_300": {
        "title": "300 активных минут",
        "description": "Накопите 300 минут тренировок и прогулок.",
        "icon_key": "fire",
        "points": 55,
        "kind": "journey",
        "target": 300,
        "unit": "мин",
    },
    "water_total_25l": {
        "title": "25 литров воды",
        "description": "Суммарно внесите 25 литров воды в дневник.",
        "icon_key": "water",
        "points": 45,
        "kind": "journey",
        "target": 25,
        "unit": "л",
    },
    "sleep_7_nights": {
        "title": "7 ночей сна",
        "description": "Запишите семь ночей сна в дневник.",
        "icon_key": "sleep",
        "points": 45,
        "kind": "journey",
        "target": 7,
        "unit": "ночей",
    },
    "longest_run": {
        "title": "Самая длинная пробежка",
        "description": "Ваш личный рекорд по дистанции за одну пробежку.",
        "icon_key": "run",
        "points": 30,
        "kind": "record",
        "unit": "км",
        "better": "higher",
    },
    "fastest_km": {
        "title": "Самый быстрый километр",
        "description": "Лучший темп на километр среди пробежек.",
        "icon_key": "speed",
        "points": 35,
        "kind": "record",
        "unit": "мин/км",
        "better": "lower",
    },
    "longest_workout": {
        "title": "Самая долгая тренировка",
        "description": "Личный рекорд по длительности активности.",
        "icon_key": "workout",
        "points": 25,
        "kind": "record",
        "unit": "мин",
        "better": "higher",
    },
    "best_steps_day": {
        "title": "Лучший день по шагам",
        "description": "Ваш максимум шагов за один день.",
        "icon_key": "steps",
        "points": 25,
        "kind": "record",
        "unit": "шагов",
        "better": "higher",
    },
}


def _has_achievement(db: Session, user_id: int, code: str) -> UA | None:
    return (
        db.query(UA)
        .filter(UA.user_id == user_id, UA.achievement_code == code)
        .first()
    )


def _fmt(value: float, unit: str | None) -> str:
    if unit == "км":
        return f"{value:.1f} км"
    if unit == "л":
        return f"{value:.1f} л"
    if unit == "мин/км":
        minutes = int(value)
        seconds = int(round((value - minutes) * 60))
        return f"{minutes}:{seconds:02d} мин/км"
    return f"{int(round(value))} {unit or ''}".strip()


def _grant(
    db: Session,
    user_id: int,
    code: str,
    *,
    progress_current: float | None = None,
    progress_target: float | None = None,
) -> UA | None:
    if code not in ACHIEVEMENT_DEFS or _has_achievement(db, user_id, code):
        return None
    d = ACHIEVEMENT_DEFS[code]
    row = UA(
        user_id=user_id,
        achievement_code=code,
        title=d["title"],
        description=d["description"],
        icon_key=d["icon_key"],
        points=d["points"],
        achievement_kind=d.get("kind", "daily"),
        progress_current=progress_current,
        progress_target=progress_target if progress_target is not None else d.get("target"),
        progress_unit=d.get("unit"),
    )
    db.add(row)
    return row


def _grant_or_update_record(db: Session, user_id: int, code: str, value: float | None) -> UA | None:
    if value is None or not isfinite(value) or value <= 0 or code not in ACHIEVEMENT_DEFS:
        return None
    d = ACHIEVEMENT_DEFS[code]
    existing = _has_achievement(db, user_id, code)
    better = d.get("better", "higher")
    improved = existing is None or (
        value > float(existing.record_value or 0) if better == "higher" else value < float(existing.record_value or value + 1)
    )
    if not improved:
        return None

    record_label = _fmt(value, d.get("unit"))
    if existing is None:
        existing = UA(
            user_id=user_id,
            achievement_code=code,
            title=d["title"],
            description=d["description"],
            icon_key=d["icon_key"],
            points=d["points"],
            achievement_kind="record",
            progress_unit=d.get("unit"),
        )
        db.add(existing)
    existing.record_value = value
    existing.record_label = record_label
    existing.progress_current = value
    existing.description = f"{d['description']} Текущий рекорд: {record_label}."
    existing.unlocked_at = datetime.now(timezone.utc)
    return existing


def _today_range() -> tuple[datetime, datetime]:
    today = datetime.now(timezone.utc).date()
    return DailySummaryService._get_day_range(today)


def _activities(db: Session, user_id: int) -> list[ActivityRecord]:
    return db.query(ActivityRecord).filter(ActivityRecord.user_id == user_id).all()


def _steps_today(db: Session, user_id: int) -> int:
    range_start, range_end = _today_range()
    activities = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= range_start,
            ActivityRecord.start_time < range_end,
        )
        .all()
    )
    walk_steps = [
        int(a.steps or 0)
        for a in activities
        if (a.activity_type or "").lower() in WALK_TYPES
    ]
    if walk_steps:
        return max(walk_steps)
    return max((int(a.steps or 0) for a in activities), default=0)


def _burn_goal(profile: UserProfile | None) -> int:
    steps = int(profile.target_steps if profile and profile.target_steps else 10_000)
    return max(400, int(steps * 0.05))


def _burned_today(db: Session, user_id: int) -> int:
    range_start, range_end = _today_range()
    activities = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= range_start,
            ActivityRecord.start_time < range_end,
        )
        .all()
    )
    training = sum(
        float(a.calories_burned or 0)
        for a in activities
        if (a.activity_type or "").lower() not in WALK_TYPES
    )
    steps = _steps_today(db, user_id)
    walk = max(
        (
            float(a.calories_burned or 0)
            for a in activities
            if (a.activity_type or "").lower() in WALK_TYPES
        ),
        default=0.0,
    )
    return int(training + (walk if walk > 0 else steps * 0.04))


def _daily_progress(db: Session, user_id: int, profile: UserProfile | None) -> dict[str, float]:
    range_start, range_end = _today_range()
    water_ml = sum(
        float(h.amount_ml or 0)
        for h in db.query(HydrationRecord)
        .filter(HydrationRecord.user_id == user_id, HydrationRecord.record_time >= range_start, HydrationRecord.record_time < range_end)
        .all()
    )
    sleep = (
        db.query(SleepRecord)
        .filter(SleepRecord.user_id == user_id, SleepRecord.sleep_end >= range_start, SleepRecord.sleep_end < range_end)
        .order_by(SleepRecord.sleep_end.desc())
        .first()
    )
    activities_today = (
        db.query(ActivityRecord)
        .filter(ActivityRecord.user_id == user_id, ActivityRecord.start_time >= range_start, ActivityRecord.start_time < range_end)
        .all()
    )
    has_training = any((a.activity_type or "").lower() not in WALK_TYPES and (a.duration_minutes or 0) >= 10 for a in activities_today)
    return {
        "steps_5k": float(_steps_today(db, user_id)),
        "steps_10k": float(_steps_today(db, user_id)),
        "water_goal": water_ml,
        "sleep_8h": float(sleep.duration_hours or 0) if sleep else 0.0,
        "burn_goal": float(_burned_today(db, user_id)),
        "first_workout": 1.0 if has_training else 0.0,
    }


def _journey_progress(db: Session, user_id: int) -> dict[str, float]:
    activities = _activities(db, user_id)
    runs = [a for a in activities if (a.activity_type or "").lower() in RUN_TYPES]
    workouts = [a for a in activities if (a.activity_type or "").lower() not in WALK_TYPES and (a.duration_minutes or 0) >= 10]
    water_l = sum(
        float(h.amount_ml or 0)
        for h in db.query(HydrationRecord).filter(HydrationRecord.user_id == user_id).all()
    ) / 1000.0
    sleep_count = db.query(SleepRecord).filter(SleepRecord.user_id == user_id).count()
    return {
        "run_total_10k": sum(float(a.distance_km or 0) for a in runs),
        "run_total_50k": sum(float(a.distance_km or 0) for a in runs),
        "workouts_10": float(len(workouts)),
        "active_minutes_300": float(sum(int(a.duration_minutes or 0) for a in activities)),
        "water_total_25l": water_l,
        "sleep_7_nights": float(sleep_count),
    }


def _record_values(db: Session, user_id: int) -> dict[str, float | None]:
    activities = _activities(db, user_id)
    runs = [a for a in activities if (a.activity_type or "").lower() in RUN_TYPES and (a.distance_km or 0) > 0]
    fastest_values = []
    for run in runs:
        if run.avg_speed_m_s and run.avg_speed_m_s > 0:
            fastest_values.append(1000.0 / float(run.avg_speed_m_s) / 60.0)
        elif run.distance_km and run.duration_minutes and run.distance_km > 0:
            fastest_values.append(float(run.duration_minutes) / float(run.distance_km))
    return {
        "longest_run": max((float(a.distance_km or 0) for a in runs), default=0.0),
        "fastest_km": min(fastest_values, default=0.0),
        "longest_workout": max((float(a.duration_minutes or 0) for a in activities), default=0.0),
        "best_steps_day": float(max((int(a.steps or 0) for a in activities), default=0)),
    }


def evaluate_and_unlock(db: Session, user_id: int) -> list[UA]:
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    newly: list[UA] = []

    daily = _daily_progress(db, user_id, profile)
    journey = _journey_progress(db, user_id)
    progress = {**daily, **journey}
    progress["steps_10k"] = daily["steps_10k"]
    progress["water_goal"] = daily["water_goal"]
    progress["burn_goal"] = daily["burn_goal"]

    dynamic_targets = {
        "steps_10k": float(profile.target_steps if profile and profile.target_steps else 10_000),
        "water_goal": float(profile.target_water_ml if profile and profile.target_water_ml else 2_500),
        "burn_goal": float(_burn_goal(profile)),
    }

    for code, value in progress.items():
        d = ACHIEVEMENT_DEFS[code]
        target = float(dynamic_targets.get(code, d.get("target", 1)))
        if value >= target:
            row = _grant(db, user_id, code, progress_current=value, progress_target=target)
            if row:
                newly.append(row)

    records = _record_values(db, user_id)
    for code, value in records.items():
        row = _grant_or_update_record(db, user_id, code, value)
        if row:
            newly.append(row)

    if newly:
        db.commit()
    return newly


def _catalog_item(
    profile: UserProfile | None,
    daily: dict[str, float],
    journey: dict[str, float],
    records: dict[str, float | None],
    code: str,
    unlocked: dict[str, UA],
) -> dict[str, Any]:
    d = ACHIEVEMENT_DEFS[code]
    row = unlocked.get(code)
    target = float(d.get("target") or 0)
    if code == "steps_10k":
        target = float(profile.target_steps if profile and profile.target_steps else 10_000)
    elif code == "water_goal":
        target = float(profile.target_water_ml if profile and profile.target_water_ml else 2_500)
    elif code == "burn_goal":
        target = float(_burn_goal(profile))

    current = records.get(code) if d.get("kind") == "record" else daily.get(code, journey.get(code, 0.0))
    item = {
        "code": code,
        "title": d["title"],
        "description": row.description if row else d["description"],
        "icon_key": d["icon_key"],
        "points": d["points"],
        "unlocked": row is not None,
        "unlocked_at": row.unlocked_at.isoformat() if row and row.unlocked_at else None,
        "kind": d.get("kind", "daily"),
        "progress_current": float(current or 0),
        "progress_target": target,
        "progress_unit": d.get("unit"),
        "record_value": float(row.record_value) if row and row.record_value is not None else None,
        "record_label": row.record_label if row else (_fmt(float(current or 0), d.get("unit")) if d.get("kind") == "record" and current else None),
    }
    return item


def refresh_user_achievements(db: Session, user_id: int) -> None:
    """Пересчитать достижения после изменения данных пользователя."""
    evaluate_and_unlock(db, user_id)


def list_achievements(db: Session, user_id: int) -> dict[str, Any]:
    refresh_user_achievements(db, user_id)
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    daily = _daily_progress(db, user_id, profile)
    journey = _journey_progress(db, user_id)
    records = _record_values(db, user_id)
    unlocked_rows = (
        db.query(UA)
        .filter(UA.user_id == user_id)
        .order_by(UA.unlocked_at.desc())
        .all()
    )
    unlocked = {u.achievement_code: u for u in unlocked_rows}
    catalog = [_catalog_item(profile, daily, journey, records, code, unlocked) for code in ACHIEVEMENT_DEFS.keys()]
    total_points = sum(u.points for u in unlocked_rows)
    return {
        "total_points": total_points,
        "unlocked_count": len(unlocked_rows),
        "total_count": len(ACHIEVEMENT_DEFS),
        "achievements": catalog,
        "recent": [
            {
                "code": u.achievement_code,
                "title": u.title,
                "description": u.description,
                "icon_key": u.icon_key,
                "points": u.points,
                "unlocked_at": u.unlocked_at.isoformat() if u.unlocked_at else None,
                "kind": u.achievement_kind,
                "record_label": u.record_label,
            }
            for u in unlocked_rows[:8]
        ],
    }
