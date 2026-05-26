"""Расчёт индекса здоровья для дашборда и аналитики."""

from datetime import date, datetime, timedelta

from sqlalchemy.orm import Session

from app.models.daily_health_summary import DailyHealthSummary
from app.models.profile import UserProfile
from app.models.user_state import UserState


def clamp_score(value: float) -> int:
    return max(0, min(100, round(value)))


def calculate_sleep_score(hours: float, target_hours: float = 8.0) -> int:
    if hours <= 0:
        return 0
    target = target_hours if target_hours > 0 else 8.0
    if hours >= target:
        return 100
    return clamp_score((hours / target) * 100)


def calculate_hydration_score(ml: float, target_ml: float = 2500.0) -> int:
    if ml <= 0:
        return 0
    target = target_ml if target_ml > 0 else 2500.0
    if ml >= target:
        return 100
    return clamp_score((ml / target) * 100)


def calculate_activity_score(steps: float, target_steps: float = 10000.0) -> int:
    if steps <= 0:
        return 0
    target = target_steps if target_steps > 0 else 10000.0
    if steps >= target:
        return 100
    return clamp_score((steps / target) * 100)


def calculate_nutrition_score_calories(calories: float, target_calories: float) -> int:
    """Прогресс по калориям за день (основной показатель питания на дашборде)."""
    if calories <= 0:
        return 0
    target = target_calories if target_calories > 0 else 2200.0
    ratio = calories / target
    if 0.85 <= ratio <= 1.15:
        return 100
    if ratio < 0.85:
        return clamp_score(ratio / 0.85 * 100)
    return clamp_score(max(40.0, 100.0 - (ratio - 1.15) * 120))


def calculate_nutrition_score_caffeine(avg_caffeine_mg: float) -> int:
    if avg_caffeine_mg <= 100:
        return 90
    if avg_caffeine_mg <= 200:
        return 75
    if avg_caffeine_mg <= 300:
        return 60
    if avg_caffeine_mg <= 400:
        return 45
    return 30


def calculate_state_score_from_states(states: list[UserState]) -> int | None:
    values: list[float] = []
    for state in states:
        for attr in ("energy", "mood", "stress", "focus", "wellbeing"):
            value = getattr(state, attr, None)
            if value is not None:
                if attr == "stress":
                    values.append(11.0 - float(value))
                else:
                    values.append(float(value))
    if not values:
        return None
    avg_state = sum(values) / len(values)
    return clamp_score((avg_state / 10.0) * 100)


def _profile_targets(db: Session, user_id: int) -> dict:
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    return {
        "sleep_hours": (profile.target_sleep_hours if profile and profile.target_sleep_hours else 8.0),
        "water_ml": int(profile.target_water_ml if profile and profile.target_water_ml else 2500),
        "steps": int(profile.target_steps if profile and profile.target_steps else 10000),
        "calories": int(profile.target_daily_calories if profile and profile.target_daily_calories else 2200),
    }


def compute_today_scores(db: Session, user_id: int) -> dict[str, int]:
    """Индекс за сегодня по фактическим целям пользователя."""
    today = date.today()
    targets = _profile_targets(db, user_id)

    summary = (
        db.query(DailyHealthSummary)
        .filter(
            DailyHealthSummary.user_id == user_id,
            DailyHealthSummary.summary_date == today,
        )
        .first()
    )

    sleep_h = summary.total_sleep_hours if summary else 0.0
    water_ml = summary.total_water_ml if summary else 0
    steps = summary.total_steps if summary else 0
    calories = summary.total_calories if summary else 0.0

    sleep_score = calculate_sleep_score(sleep_h, targets["sleep_hours"])
    hydration_score = calculate_hydration_score(float(water_ml), float(targets["water_ml"]))
    activity_score = calculate_activity_score(float(steps), float(targets["steps"]))
    nutrition_score = calculate_nutrition_score_calories(calories, float(targets["calories"]))

    day_start = datetime.combine(today, datetime.min.time())
    day_end = datetime.combine(today, datetime.max.time())
    states = (
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= day_start,
            UserState.record_time <= day_end,
        )
        .all()
    )
    state_raw = calculate_state_score_from_states(states)
    state_score = state_raw if state_raw is not None else 75

    parts = [sleep_score, hydration_score, activity_score, nutrition_score, state_score]
    health_score = clamp_score(sum(parts) / len(parts))

    return {
        "health_score": health_score,
        "sleep_score": sleep_score,
        "hydration_score": hydration_score,
        "activity_score": activity_score,
        "nutrition_score": nutrition_score,
        "state_score": state_score,
    }


def compute_period_average_scores(
    db: Session,
    user_id: int,
    start_date: date,
    end_date: date,
) -> dict[str, int]:
    summaries = (
        db.query(DailyHealthSummary)
        .filter(
            DailyHealthSummary.user_id == user_id,
            DailyHealthSummary.summary_date >= start_date,
            DailyHealthSummary.summary_date <= end_date,
        )
        .all()
    )

    if summaries:
        avg_sleep = sum(s.total_sleep_hours or 0 for s in summaries) / len(summaries)
        avg_water = sum(s.total_water_ml or 0 for s in summaries) / len(summaries)
        avg_steps = sum(s.total_steps or 0 for s in summaries) / len(summaries)
        avg_caffeine = sum(s.total_caffeine_mg or 0 for s in summaries) / len(summaries)
    else:
        avg_sleep = avg_water = avg_steps = avg_caffeine = 0.0

    sleep_score = calculate_sleep_score(avg_sleep)
    hydration_score = calculate_hydration_score(avg_water)
    activity_score = calculate_activity_score(avg_steps)
    nutrition_score = calculate_nutrition_score_caffeine(avg_caffeine)
    state_score = calculate_state_score_from_states(
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= datetime.combine(start_date, datetime.min.time()),
            UserState.record_time <= datetime.combine(end_date, datetime.max.time()),
        )
        .all()
    ) or 50

    health_score = clamp_score(
        (sleep_score + hydration_score + activity_score + nutrition_score + state_score) / 5
    )

    return {
        "health_score": health_score,
        "sleep_score": sleep_score,
        "hydration_score": hydration_score,
        "activity_score": activity_score,
        "nutrition_score": nutrition_score,
        "state_score": state_score,
    }
