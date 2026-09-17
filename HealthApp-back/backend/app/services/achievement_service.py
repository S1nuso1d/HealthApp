"""Проверка и выдача достижений, сезонная таблица и ранги."""

from __future__ import annotations

from calendar import monthrange
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone
from math import isfinite
from typing import Any

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.gamification import UserAchievement as UA
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.models.user import User
from app.services.analytics.daily_summary_service import DailySummaryService

WALK_TYPES = {"walk", "walking", "ходьба"}
RUN_TYPES = {"run", "running", "jogging", "бег", "пробежка"}

# Ранги турнира по сезонным очкам (текущий месяц).
TIER_DEFS: list[dict[str, Any]] = [
    {"code": "bronze", "title": "Бронза", "min_points": 0},
    {"code": "silver", "title": "Серебро", "min_points": 120},
    {"code": "gold", "title": "Золото", "min_points": 280},
    {"code": "platinum", "title": "Платина", "min_points": 500},
    {"code": "diamond", "title": "Алмаз", "min_points": 850},
    {"code": "legend", "title": "Легенда", "min_points": 1300},
]

POINT_RULES: list[dict[str, Any]] = [
    {"code": "log_water", "title": "Запись воды", "points": 5, "unit": "день"},
    {"code": "log_sleep", "title": "Запись сна", "points": 8, "unit": "день"},
    {"code": "log_meal", "title": "Запись питания", "points": 6, "unit": "день"},
    {"code": "log_workout", "title": "Тренировка ≥10 мин", "points": 12, "unit": "день"},
    {"code": "goal_steps", "title": "Цель по шагам", "points": 10, "unit": "день"},
    {"code": "goal_water", "title": "Норма воды", "points": 8, "unit": "день"},
    {"code": "goal_sleep", "title": "Сон ≥7 ч", "points": 10, "unit": "день"},
    {"code": "full_day", "title": "Полный день (всё заполнено)", "points": 15, "unit": "день"},
]

ACHIEVEMENT_DEFS: dict[str, dict[str, Any]] = {
    # —— Ежедневные ——
    "steps_3k": {
        "title": "3 000 шагов",
        "description": "Лёгкая активность за день — уже хороший старт.",
        "icon_key": "steps",
        "points": 8,
        "kind": "daily",
        "target": 3_000,
        "unit": "шагов",
    },
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
    "steps_15k": {
        "title": "15 000 шагов",
        "description": "Очень активный день — вышли далеко за обычную норму.",
        "icon_key": "steps",
        "points": 45,
        "kind": "daily",
        "target": 15_000,
        "unit": "шагов",
    },
    "water_500": {
        "title": "Первые 500 мл",
        "description": "Начали день с воды — привычка формируется.",
        "icon_key": "water",
        "points": 5,
        "kind": "daily",
        "target": 500,
        "unit": "мл",
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
    "sleep_7h": {
        "title": "7 часов сна",
        "description": "Базовый минимум для восстановления.",
        "icon_key": "sleep",
        "points": 15,
        "kind": "daily",
        "target": 7,
        "unit": "ч",
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
    "double_workout": {
        "title": "Две тренировки за день",
        "description": "Два занятия от 10 минут в один день.",
        "icon_key": "workout",
        "points": 30,
        "kind": "daily",
        "target": 2,
        "unit": "тренировки",
    },
    "meal_logged": {
        "title": "Дневник питания",
        "description": "Отметили хотя бы один приём пищи сегодня.",
        "icon_key": "apple",
        "points": 8,
        "kind": "daily",
        "target": 1,
        "unit": "запись",
    },
    "meals_3": {
        "title": "Три приёма пищи",
        "description": "Записали завтрак, обед и ужин (или 3+ приёма).",
        "icon_key": "apple",
        "points": 18,
        "kind": "daily",
        "target": 3,
        "unit": "приёма",
    },
    "nutrition_perfect_day": {
        "title": "Идеальное питание",
        "description": "Попали в цель по калориям с погрешностью до 15%.",
        "icon_key": "apple",
        "points": 35,
        "kind": "daily",
        "target": 1,
        "unit": "день",
    },
    "early_bird": {
        "title": "Ранняя пташка",
        "description": "Легли спать до 23:00.",
        "icon_key": "moon",
        "points": 25,
        "kind": "daily",
        "target": 1,
        "unit": "раз",
    },
    "full_log_day": {
        "title": "Полный день",
        "description": "Вода, сон, питание и активность — всё записано за день.",
        "icon_key": "star",
        "points": 40,
        "kind": "daily",
        "target": 1,
        "unit": "день",
    },
    # —— Ежемесячные ——
    "month_water_10": {
        "title": "Вода 10 дней",
        "description": "В этом месяце отметили воду минимум 10 дней.",
        "icon_key": "water",
        "points": 40,
        "kind": "monthly",
        "target": 10,
        "unit": "дней",
    },
    "month_water_20": {
        "title": "Вода 20 дней",
        "description": "Регулярная гидратация почти весь месяц.",
        "icon_key": "water",
        "points": 70,
        "kind": "monthly",
        "target": 20,
        "unit": "дней",
    },
    "month_sleep_10": {
        "title": "10 ночей сна",
        "description": "Записали сон в дневник 10 раз за месяц.",
        "icon_key": "sleep",
        "points": 45,
        "kind": "monthly",
        "target": 10,
        "unit": "ночей",
    },
    "month_sleep_20": {
        "title": "20 ночей сна",
        "description": "Сон под контролем почти каждый день месяца.",
        "icon_key": "sleep",
        "points": 80,
        "kind": "monthly",
        "target": 20,
        "unit": "ночей",
    },
    "month_workouts_8": {
        "title": "8 тренировок в месяце",
        "description": "Восемь занятий от 10 минут за календарный месяц.",
        "icon_key": "workout",
        "points": 60,
        "kind": "monthly",
        "target": 8,
        "unit": "тренировок",
    },
    "month_workouts_15": {
        "title": "15 тренировок в месяце",
        "description": "Серьёзный тренировочный месяц.",
        "icon_key": "workout",
        "points": 100,
        "kind": "monthly",
        "target": 15,
        "unit": "тренировок",
    },
    "month_meals_15": {
        "title": "Питание 15 дней",
        "description": "Вели дневник еды минимум 15 дней в месяце.",
        "icon_key": "apple",
        "points": 50,
        "kind": "monthly",
        "target": 15,
        "unit": "дней",
    },
    "month_steps_goal_10": {
        "title": "Цель шагов 10 дней",
        "description": "Достигли дневной цели по шагам 10 раз за месяц.",
        "icon_key": "steps",
        "points": 55,
        "kind": "monthly",
        "target": 10,
        "unit": "дней",
    },
    "month_full_log_5": {
        "title": "5 полных дней",
        "description": "Пять дней, когда заполнены вода, сон, еда и активность.",
        "icon_key": "star",
        "points": 75,
        "kind": "monthly",
        "target": 5,
        "unit": "дней",
    },
    "month_active_minutes_400": {
        "title": "400 минут активности",
        "description": "Суммарно 400+ минут тренировок и ходьбы за месяц.",
        "icon_key": "workout",
        "points": 65,
        "kind": "monthly",
        "target": 400,
        "unit": "мин",
    },
    "month_season_bronze": {
        "title": "Сезон: бронза",
        "description": "Наберите 120 сезонных очков за месяц.",
        "icon_key": "trophy",
        "points": 20,
        "kind": "monthly",
        "target": 120,
        "unit": "очков",
    },
    "month_season_gold": {
        "title": "Сезон: золото",
        "description": "Наберите 280 сезонных очков за месяц.",
        "icon_key": "trophy",
        "points": 50,
        "kind": "monthly",
        "target": 280,
        "unit": "очков",
    },
    # —— Путь ——
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
    "run_total_100k": {
        "title": "100 км бега суммарно",
        "description": "Сотня километров — серьёзный беговой путь.",
        "icon_key": "run",
        "points": 150,
        "kind": "journey",
        "target": 100,
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
    "workouts_50": {
        "title": "50 тренировок",
        "description": "Полсотни занятий в дневнике — устойчивая привычка.",
        "icon_key": "workout",
        "points": 120,
        "kind": "journey",
        "target": 50,
        "unit": "тренировок",
    },
    "active_minutes_300": {
        "title": "300 минут активности",
        "description": "Суммарно наберите 300 минут активности за всё время.",
        "icon_key": "workout",
        "points": 55,
        "kind": "journey",
        "target": 300,
        "unit": "мин",
    },
    "active_minutes_1000": {
        "title": "1 000 минут активности",
        "description": "Тысяча минут движения в дневнике.",
        "icon_key": "workout",
        "points": 110,
        "kind": "journey",
        "target": 1_000,
        "unit": "мин",
    },
    "water_streak_7": {
        "title": "Водный марафон (7 дней)",
        "description": "Пили свою норму воды 7 дней подряд.",
        "icon_key": "water",
        "points": 60,
        "kind": "journey",
        "target": 7,
        "unit": "дней",
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
    "water_total_100l": {
        "title": "100 литров воды",
        "description": "Сотня литров в дневнике гидратации.",
        "icon_key": "water",
        "points": 100,
        "kind": "journey",
        "target": 100,
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
    "sleep_30_nights": {
        "title": "30 ночей сна",
        "description": "Месяц записей сна суммарно за всё время.",
        "icon_key": "sleep",
        "points": 90,
        "kind": "journey",
        "target": 30,
        "unit": "ночей",
    },
    "meals_100": {
        "title": "100 записей еды",
        "description": "Сто приёмов пищи в дневнике питания.",
        "icon_key": "apple",
        "points": 70,
        "kind": "journey",
        "target": 100,
        "unit": "записей",
    },
    # —— Рекорды ——
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
    "best_water_day": {
        "title": "Лучший день по воде",
        "description": "Максимум выпитой воды за один день.",
        "icon_key": "water",
        "points": 20,
        "kind": "record",
        "unit": "мл",
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
        value > float(existing.record_value or 0)
        if better == "higher"
        else value < float(existing.record_value or value + 1)
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


def _month_bounds(day: date | None = None) -> tuple[datetime, datetime, str]:
    day = day or datetime.now(timezone.utc).date()
    start_d = day.replace(day=1)
    last = monthrange(day.year, day.month)[1]
    end_d = day.replace(day=last) + timedelta(days=1)
    start = datetime(start_d.year, start_d.month, start_d.day, tzinfo=timezone.utc)
    end = datetime(end_d.year, end_d.month, end_d.day, tzinfo=timezone.utc)
    label = start_d.strftime("%Y-%m")
    return start, end, label


def _activities(db: Session, user_id: int) -> list[ActivityRecord]:
    return db.query(ActivityRecord).filter(ActivityRecord.user_id == user_id).all()


def _steps_on_day(activities: list[ActivityRecord]) -> int:
    walk_steps = [
        int(a.steps or 0)
        for a in activities
        if (a.activity_type or "").lower() in WALK_TYPES
    ]
    if walk_steps:
        return max(walk_steps)
    return max((int(a.steps or 0) for a in activities), default=0)


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
    return _steps_on_day(activities)


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
    steps = _steps_on_day(activities)
    walk = max(
        (
            float(a.calories_burned or 0)
            for a in activities
            if (a.activity_type or "").lower() in WALK_TYPES
        ),
        default=0.0,
    )
    return int(training + (walk if walk > 0 else steps * 0.04))


def _water_streak(db: Session, user_id: int, profile: UserProfile | None) -> int:
    target = float(profile.target_water_ml if profile and profile.target_water_ml else 2_500)
    rows = (
        db.query(HydrationRecord)
        .filter(HydrationRecord.user_id == user_id)
        .all()
    )
    by_day: dict[date, float] = defaultdict(float)
    for h in rows:
        if h.record_time is None:
            continue
        day = h.record_time.astimezone(timezone.utc).date()
        by_day[day] += float(h.amount_ml or 0)
    streak = 0
    cursor = datetime.now(timezone.utc).date()
    while by_day.get(cursor, 0) >= target:
        streak += 1
        cursor -= timedelta(days=1)
    return streak


def _local_hour(dt: datetime) -> int:
    if dt.tzinfo is None:
        return dt.hour
    return dt.astimezone(timezone.utc).hour


def _daily_progress(db: Session, user_id: int, profile: UserProfile | None) -> dict[str, float]:
    range_start, range_end = _today_range()
    water_ml = sum(
        float(h.amount_ml or 0)
        for h in db.query(HydrationRecord)
        .filter(
            HydrationRecord.user_id == user_id,
            HydrationRecord.record_time >= range_start,
            HydrationRecord.record_time < range_end,
        )
        .all()
    )
    sleep = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == user_id,
            SleepRecord.sleep_end >= range_start,
            SleepRecord.sleep_end < range_end,
        )
        .order_by(SleepRecord.sleep_end.desc())
        .first()
    )
    activities_today = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= range_start,
            ActivityRecord.start_time < range_end,
        )
        .all()
    )
    meals_today = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == user_id,
            MealRecord.meal_time >= range_start,
            MealRecord.meal_time < range_end,
        )
        .all()
    )
    workouts = [
        a
        for a in activities_today
        if (a.activity_type or "").lower() not in WALK_TYPES and (a.duration_minutes or 0) >= 10
    ]
    meal_calories = sum(float(m.calories or 0) for m in meals_today)
    cal_target = float(profile.target_daily_calories if profile and profile.target_daily_calories else 0)
    nutrition_ok = 0.0
    if cal_target > 0 and meal_calories > 0:
        ratio = meal_calories / cal_target
        if 0.85 <= ratio <= 1.15:
            nutrition_ok = 1.0
    early = 0.0
    if sleep and sleep.sleep_start is not None:
        hour = _local_hour(sleep.sleep_start)
        if 18 <= hour <= 22:
            early = 1.0

    has_water = water_ml > 0
    has_sleep = sleep is not None
    has_meal = len(meals_today) > 0
    has_activity = len(activities_today) > 0
    full_log = 1.0 if has_water and has_sleep and has_meal and has_activity else 0.0

    return {
        "steps_3k": float(_steps_on_day(activities_today)),
        "steps_5k": float(_steps_on_day(activities_today)),
        "steps_10k": float(_steps_on_day(activities_today)),
        "steps_15k": float(_steps_on_day(activities_today)),
        "water_500": water_ml,
        "water_goal": water_ml,
        "sleep_7h": float(sleep.duration_hours or 0) if sleep else 0.0,
        "sleep_8h": float(sleep.duration_hours or 0) if sleep else 0.0,
        "burn_goal": float(_burned_today(db, user_id)),
        "first_workout": 1.0 if workouts else 0.0,
        "double_workout": float(len(workouts)),
        "meal_logged": 1.0 if meals_today else 0.0,
        "meals_3": float(len(meals_today)),
        "nutrition_perfect_day": nutrition_ok,
        "early_bird": early,
        "full_log_day": full_log,
    }


def _day_buckets_for_month(
    db: Session,
    user_id: int,
    profile: UserProfile | None,
) -> dict[str, Any]:
    start, end, label = _month_bounds()
    water_rows = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.user_id == user_id,
            HydrationRecord.record_time >= start,
            HydrationRecord.record_time < end,
        )
        .all()
    )
    sleep_rows = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == user_id,
            SleepRecord.sleep_end >= start,
            SleepRecord.sleep_end < end,
        )
        .all()
    )
    meal_rows = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == user_id,
            MealRecord.meal_time >= start,
            MealRecord.meal_time < end,
        )
        .all()
    )
    activity_rows = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= start,
            ActivityRecord.start_time < end,
        )
        .all()
    )

    water_by_day: dict[date, float] = defaultdict(float)
    for h in water_rows:
        water_by_day[h.record_time.astimezone(timezone.utc).date()] += float(h.amount_ml or 0)

    sleep_days = {s.sleep_end.astimezone(timezone.utc).date() for s in sleep_rows}
    sleep_hours: dict[date, float] = {}
    for s in sleep_rows:
        d = s.sleep_end.astimezone(timezone.utc).date()
        sleep_hours[d] = max(sleep_hours.get(d, 0.0), float(s.duration_hours or 0))

    meal_days = {m.meal_time.astimezone(timezone.utc).date() for m in meal_rows}

    acts_by_day: dict[date, list[ActivityRecord]] = defaultdict(list)
    for a in activity_rows:
        acts_by_day[a.start_time.astimezone(timezone.utc).date()].append(a)

    water_target = float(profile.target_water_ml if profile and profile.target_water_ml else 2_500)
    steps_target = float(profile.target_steps if profile and profile.target_steps else 10_000)

    return {
        "label": label,
        "water_by_day": water_by_day,
        "sleep_days": sleep_days,
        "sleep_hours": sleep_hours,
        "meal_days": meal_days,
        "acts_by_day": acts_by_day,
        "activity_rows": activity_rows,
        "water_target": water_target,
        "steps_target": steps_target,
    }


def compute_season_score(db: Session, user_id: int, profile: UserProfile | None = None) -> dict[str, Any]:
    """Очки сезона (месяца) за ведение дневника и выполнение целей."""
    if profile is None:
        profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    buckets = _day_buckets_for_month(db, user_id, profile)
    water_by_day = buckets["water_by_day"]
    sleep_days = buckets["sleep_days"]
    sleep_hours = buckets["sleep_hours"]
    meal_days = buckets["meal_days"]
    acts_by_day = buckets["acts_by_day"]
    water_target = buckets["water_target"]
    steps_target = buckets["steps_target"]

    all_days = set(water_by_day) | sleep_days | meal_days | set(acts_by_day)
    points = 0
    breakdown = {r["code"]: 0 for r in POINT_RULES}

    for day in all_days:
        day_acts = acts_by_day.get(day, [])
        has_water = water_by_day.get(day, 0) > 0
        has_sleep = day in sleep_days
        has_meal = day in meal_days
        has_workout = any(
            (a.activity_type or "").lower() not in WALK_TYPES and (a.duration_minutes or 0) >= 10
            for a in day_acts
        )
        has_any_activity = len(day_acts) > 0
        steps = _steps_on_day(day_acts)

        if has_water:
            points += 5
            breakdown["log_water"] += 5
        if has_sleep:
            points += 8
            breakdown["log_sleep"] += 8
        if has_meal:
            points += 6
            breakdown["log_meal"] += 6
        if has_workout:
            points += 12
            breakdown["log_workout"] += 12
        if steps >= steps_target:
            points += 10
            breakdown["goal_steps"] += 10
        if water_by_day.get(day, 0) >= water_target:
            points += 8
            breakdown["goal_water"] += 8
        if sleep_hours.get(day, 0) >= 7:
            points += 10
            breakdown["goal_sleep"] += 10
        if has_water and has_sleep and has_meal and (has_workout or has_any_activity):
            points += 15
            breakdown["full_day"] += 15

    tier = resolve_tier(points)
    return {
        "season_points": points,
        "season_label": buckets["label"],
        "breakdown": breakdown,
        "tier": tier,
    }


def resolve_tier(points: int) -> dict[str, Any]:
    current = TIER_DEFS[0]
    for tier in TIER_DEFS:
        if points >= tier["min_points"]:
            current = tier
    idx = next(i for i, t in enumerate(TIER_DEFS) if t["code"] == current["code"])
    nxt = TIER_DEFS[idx + 1] if idx + 1 < len(TIER_DEFS) else None
    return {
        "code": current["code"],
        "title": current["title"],
        "min_points": current["min_points"],
        "next_code": nxt["code"] if nxt else None,
        "next_title": nxt["title"] if nxt else None,
        "points_to_next": (nxt["min_points"] - points) if nxt else None,
    }


def _monthly_progress(db: Session, user_id: int, profile: UserProfile | None) -> dict[str, float]:
    buckets = _day_buckets_for_month(db, user_id, profile)
    water_by_day = buckets["water_by_day"]
    sleep_days = buckets["sleep_days"]
    meal_days = buckets["meal_days"]
    acts_by_day = buckets["acts_by_day"]
    activity_rows = buckets["activity_rows"]
    water_target = buckets["water_target"]
    steps_target = buckets["steps_target"]

    water_days = sum(1 for v in water_by_day.values() if v > 0)
    workouts = [
        a
        for a in activity_rows
        if (a.activity_type or "").lower() not in WALK_TYPES and (a.duration_minutes or 0) >= 10
    ]
    steps_goal_days = 0
    full_days = 0
    for day, acts in acts_by_day.items():
        if _steps_on_day(acts) >= steps_target:
            steps_goal_days += 1
    all_days = set(water_by_day) | sleep_days | meal_days | set(acts_by_day)
    for day in all_days:
        has_water = water_by_day.get(day, 0) > 0
        has_sleep = day in sleep_days
        has_meal = day in meal_days
        has_act = day in acts_by_day
        if has_water and has_sleep and has_meal and has_act:
            full_days += 1

    season = compute_season_score(db, user_id, profile)
    active_minutes = float(sum(int(a.duration_minutes or 0) for a in activity_rows))

    return {
        "month_water_10": float(water_days),
        "month_water_20": float(water_days),
        "month_sleep_10": float(len(sleep_days)),
        "month_sleep_20": float(len(sleep_days)),
        "month_workouts_8": float(len(workouts)),
        "month_workouts_15": float(len(workouts)),
        "month_meals_15": float(len(meal_days)),
        "month_steps_goal_10": float(steps_goal_days),
        "month_full_log_5": float(full_days),
        "month_active_minutes_400": active_minutes,
        "month_season_bronze": float(season["season_points"]),
        "month_season_gold": float(season["season_points"]),
    }


def _journey_progress(db: Session, user_id: int, profile: UserProfile | None) -> dict[str, float]:
    activities = _activities(db, user_id)
    runs = [a for a in activities if (a.activity_type or "").lower() in RUN_TYPES]
    workouts = [
        a
        for a in activities
        if (a.activity_type or "").lower() not in WALK_TYPES and (a.duration_minutes or 0) >= 10
    ]
    water_l = (
        sum(
            float(h.amount_ml or 0)
            for h in db.query(HydrationRecord).filter(HydrationRecord.user_id == user_id).all()
        )
        / 1000.0
    )
    sleep_count = db.query(SleepRecord).filter(SleepRecord.user_id == user_id).count()
    meals_count = db.query(MealRecord).filter(MealRecord.user_id == user_id).count()
    return {
        "run_total_10k": sum(float(a.distance_km or 0) for a in runs),
        "run_total_50k": sum(float(a.distance_km or 0) for a in runs),
        "run_total_100k": sum(float(a.distance_km or 0) for a in runs),
        "workouts_10": float(len(workouts)),
        "workouts_50": float(len(workouts)),
        "active_minutes_300": float(sum(int(a.duration_minutes or 0) for a in activities)),
        "active_minutes_1000": float(sum(int(a.duration_minutes or 0) for a in activities)),
        "water_streak_7": float(_water_streak(db, user_id, profile)),
        "water_total_25l": water_l,
        "water_total_100l": water_l,
        "sleep_7_nights": float(sleep_count),
        "sleep_30_nights": float(sleep_count),
        "meals_100": float(meals_count),
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

    water_rows = db.query(HydrationRecord).filter(HydrationRecord.user_id == user_id).all()
    water_by_day: dict[date, float] = defaultdict(float)
    for h in water_rows:
        if h.record_time is None:
            continue
        water_by_day[h.record_time.astimezone(timezone.utc).date()] += float(h.amount_ml or 0)

    return {
        "longest_run": max((float(a.distance_km or 0) for a in runs), default=0.0),
        "fastest_km": min(fastest_values, default=0.0),
        "longest_workout": max((float(a.duration_minutes or 0) for a in activities), default=0.0),
        "best_steps_day": float(max((int(a.steps or 0) for a in activities), default=0)),
        "best_water_day": float(max(water_by_day.values(), default=0.0)),
    }


def evaluate_and_unlock(db: Session, user_id: int) -> list[UA]:
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    newly: list[UA] = []

    daily = _daily_progress(db, user_id, profile)
    monthly = _monthly_progress(db, user_id, profile)
    journey = _journey_progress(db, user_id, profile)
    progress = {**daily, **monthly, **journey}

    dynamic_targets = {
        "steps_10k": float(profile.target_steps if profile and profile.target_steps else 10_000),
        "water_goal": float(profile.target_water_ml if profile and profile.target_water_ml else 2_500),
        "burn_goal": float(_burn_goal(profile)),
    }

    for code, value in progress.items():
        if code not in ACHIEVEMENT_DEFS:
            continue
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
    monthly: dict[str, float],
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

    kind = d.get("kind", "daily")
    if kind == "record":
        current = records.get(code)
    elif kind == "monthly":
        current = monthly.get(code, 0.0)
    elif kind == "journey":
        current = journey.get(code, 0.0)
    else:
        current = daily.get(code, 0.0)

    return {
        "code": code,
        "title": d["title"],
        "description": row.description if row else d["description"],
        "icon_key": d["icon_key"],
        "points": d["points"],
        "unlocked": row is not None,
        "unlocked_at": row.unlocked_at.isoformat() if row and row.unlocked_at else None,
        "kind": kind,
        "progress_current": float(current or 0),
        "progress_target": target,
        "progress_unit": d.get("unit"),
        "record_value": float(row.record_value) if row and row.record_value is not None else None,
        "record_label": row.record_label
        if row
        else (
            _fmt(float(current or 0), d.get("unit"))
            if kind == "record" and current
            else None
        ),
    }


def _display_name(profile: UserProfile | None, user: User | None) -> str:
    if profile:
        if profile.nickname and profile.nickname.strip():
            return profile.nickname.strip()
        name = " ".join(
            p for p in [profile.first_name or "", profile.last_name or ""] if p.strip()
        ).strip()
        if name:
            return name
    if user and user.email:
        return user.email.split("@")[0]
    return "Игрок"


def build_leaderboard(db: Session, current_user_id: int, limit: int = 20) -> list[dict[str, Any]]:
    """Турнир по сезонным очкам среди пользователей с профилем."""
    users = db.query(User).filter(User.is_active.is_(True)).limit(200).all()
    scored: list[dict[str, Any]] = []
    for user in users:
        profile = db.query(UserProfile).filter(UserProfile.user_id == user.id).first()
        season = compute_season_score(db, user.id, profile)
        pts = int(season["season_points"])
        if pts <= 0 and user.id != current_user_id:
            continue
        tier = season["tier"]
        scored.append(
            {
                "user_id": user.id,
                "display_name": _display_name(profile, user),
                "season_points": pts,
                "tier_code": tier["code"],
                "tier_title": tier["title"],
                "is_self": user.id == current_user_id,
            }
        )
    scored.sort(key=lambda x: (-x["season_points"], x["display_name"].lower()))
    for i, row in enumerate(scored, start=1):
        row["rank"] = i
    top = scored[:limit]
    self_row = next((r for r in scored if r["is_self"]), None)
    if self_row and self_row["user_id"] not in {r["user_id"] for r in top}:
        top.append(self_row)
    return top


def refresh_user_achievements(db: Session, user_id: int) -> None:
    """Пересчитать достижения после изменения данных пользователя."""
    evaluate_and_unlock(db, user_id)


def list_achievements(db: Session, user_id: int) -> dict[str, Any]:
    refresh_user_achievements(db, user_id)
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    daily = _daily_progress(db, user_id, profile)
    monthly = _monthly_progress(db, user_id, profile)
    journey = _journey_progress(db, user_id, profile)
    records = _record_values(db, user_id)
    unlocked_rows = (
        db.query(UA)
        .filter(UA.user_id == user_id)
        .order_by(UA.unlocked_at.desc())
        .all()
    )
    unlocked = {u.achievement_code: u for u in unlocked_rows}
    catalog = [
        _catalog_item(profile, daily, monthly, journey, records, code, unlocked)
        for code in ACHIEVEMENT_DEFS.keys()
    ]
    season = compute_season_score(db, user_id, profile)
    total_points = sum(u.points for u in unlocked_rows)
    return {
        "total_points": total_points,
        "unlocked_count": len(unlocked_rows),
        "total_count": len(ACHIEVEMENT_DEFS),
        "season_points": season["season_points"],
        "season_label": season["season_label"],
        "tier": season["tier"],
        "point_rules": POINT_RULES,
        "tiers": TIER_DEFS,
        "leaderboard": build_leaderboard(db, user_id),
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
