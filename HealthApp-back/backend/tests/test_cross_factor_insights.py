from datetime import datetime, timedelta, timezone

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.services.correlation_analyzer import CorrelationAnalyzer


def test_cross_factors_detect_late_workout_and_late_meal(db_session, test_user):
    """Поздняя тренировка + поздний ужин должны всплыть как связка, а не только по отдельности."""
    now = datetime.now(timezone.utc)
    user_id = test_user.id

    for i in range(8):
        day = now - timedelta(days=i)
        is_bad = i % 2 == 0
        sleep_hours = 5.6 if is_bad else 7.8
        sleep_end = day.replace(hour=7, minute=0)
        db_session.add(
            SleepRecord(
                user_id=user_id,
                sleep_start=sleep_end - timedelta(hours=sleep_hours),
                sleep_end=sleep_end,
                duration_hours=sleep_hours,
                quality_score=4 if is_bad else 8,
            )
        )
        db_session.add(
            MealRecord(
                user_id=user_id,
                meal_type="dinner",
                name="Ужин",
                calories=800,
                protein_g=30,
                meal_time=day.replace(hour=22 if is_bad else 18, minute=0),
                is_late_meal=is_bad,
                minutes_before_sleep=60 if is_bad else 240,
                caffeine_mg=80 if is_bad else 0,
            )
        )
        db_session.add(
            ActivityRecord(
                user_id=user_id,
                activity_type="gym",
                start_time=day.replace(hour=21 if is_bad else 12, minute=0),
                end_time=day.replace(hour=22 if is_bad else 13, minute=0),
                duration_minutes=50,
                steps=9000 if not is_bad else 3000,
                intensity="high" if is_bad else "medium",
                is_evening_activity=is_bad,
                minutes_before_sleep=90 if is_bad else 500,
            )
        )
        db_session.add(
            HydrationRecord(
                user_id=user_id,
                amount_ml=1200 if is_bad else 2200,
                drink_type="water",
                record_time=day.replace(hour=15, minute=0),
            )
        )
        db_session.add(
            UserState(
                user_id=user_id,
                energy=4 if is_bad else 8,
                mood=4 if is_bad else 8,
                stress=8 if is_bad else 3,
                focus=4 if is_bad else 8,
                wellbeing=4 if is_bad else 8,
                record_time=day.replace(hour=12, minute=0),
            )
        )

    db_session.commit()

    insights = CorrelationAnalyzer.analyze_correlations(
        db=db_session,
        user_id=user_id,
        period_days=14,
    )
    types = {item["insight_type"] for item in insights}

    assert "late_meal_and_evening_workout_sleep" in types
    assert types & {
        "late_meal_sleep_impact",
        "evening_high_activity_sleep_impact",
        "short_sleep_low_energy",
        "low_steps_low_energy",
        "short_sleep_low_mood",
    }


def test_recovery_stack_positive_pattern(db_session, test_user):
    now = datetime.now(timezone.utc)
    user_id = test_user.id
    for i in range(8):
        day = now - timedelta(days=i)
        good = i < 4
        db_session.add(
            SleepRecord(
                user_id=user_id,
                sleep_start=day - timedelta(hours=8 if good else 5),
                sleep_end=day.replace(hour=7, minute=0),
                duration_hours=8.0 if good else 5.5,
            )
        )
        db_session.add(
            HydrationRecord(
                user_id=user_id,
                amount_ml=2200 if good else 900,
                drink_type="water",
                record_time=day.replace(hour=14, minute=0),
            )
        )
        db_session.add(
            ActivityRecord(
                user_id=user_id,
                activity_type="walk",
                start_time=day.replace(hour=12, minute=0),
                end_time=day.replace(hour=13, minute=0),
                duration_minutes=40,
                steps=9000 if good else 2000,
                intensity="low",
            )
        )
        db_session.add(
            UserState(
                user_id=user_id,
                energy=8 if good else 4,
                mood=8 if good else 4,
                record_time=day.replace(hour=16, minute=0),
            )
        )
    db_session.commit()

    insights = CorrelationAnalyzer.analyze_correlations(db_session, user_id, period_days=14)
    types = {item["insight_type"] for item in insights}
    assert "recovery_stack_high_energy" in types or "high_steps_high_energy" in types
    assert "good_sleep_high_energy" in types or "short_sleep_low_energy" in types
