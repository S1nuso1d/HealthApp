from datetime import datetime, timedelta, timezone

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.services.health_score_service import (
    calculate_activity_score,
    calculate_hydration_score,
    calculate_nutrition_score_calories,
    calculate_sleep_score,
    compute_today_scores,
)


def test_compute_today_scores_empty_db(db_session, test_user):
    """Пустой день даёт нули, а не «средний» балл: индекс считается только по
    категориям, куда пользователь что-то записал."""
    scores = compute_today_scores(db_session, test_user.id)
    assert scores["sleep_score"] == 0
    assert scores["hydration_score"] == 0
    assert scores["activity_score"] == 0
    assert scores["nutrition_score"] == 0
    assert scores["state_score"] == 0
    assert scores["health_score"] == 0


def test_compute_today_scores_with_data(db_session, test_user):
    now = datetime.now(timezone.utc)

    db_session.add(
        SleepRecord(
            user_id=test_user.id,
            sleep_start=now - timedelta(hours=8),
            sleep_end=now,
            duration_hours=8.0,
        )
    )
    db_session.add(
        HydrationRecord(user_id=test_user.id, amount_ml=2500, record_time=now)
    )
    db_session.add(
        ActivityRecord(
            user_id=test_user.id,
            activity_type="walking",
            steps=10000,
            start_time=now - timedelta(hours=1),
            end_time=now,
            duration_minutes=60,
        )
    )
    db_session.commit()

    scores = compute_today_scores(db_session, test_user.id)
    assert scores["sleep_score"] == 100
    assert scores["hydration_score"] == 100
    assert scores["activity_score"] == 100
    # Еды нет — категория в среднее не попадает, поэтому индекс всё равно 100
    assert scores["nutrition_score"] == 0
    assert scores["health_score"] == 100


def test_compute_today_scores_uses_profile_targets(db_session, test_user):
    """Цели из профиля важнее дефолтов: 5000 шагов при цели 5000 — это 100%."""
    db_session.add(
        UserProfile(
            user_id=test_user.id,
            target_steps=5000,
            target_water_ml=1500,
            target_sleep_hours=6.0,
        )
    )
    now = datetime.now(timezone.utc)
    db_session.add(
        ActivityRecord(
            user_id=test_user.id,
            activity_type="walking",
            steps=5000,
            start_time=now - timedelta(hours=1),
            end_time=now,
            duration_minutes=60,
        )
    )
    db_session.commit()

    scores = compute_today_scores(db_session, test_user.id)
    assert scores["activity_score"] == 100


def test_compute_today_scores_counts_meal_calories(db_session, test_user):
    now = datetime.now(timezone.utc)
    db_session.add(
        UserProfile(user_id=test_user.id, target_daily_calories=2000)
    )
    db_session.add(
        MealRecord(
            user_id=test_user.id,
            meal_type="lunch",
            name="Обед",
            calories=2000,
            meal_time=now,
        )
    )
    db_session.commit()

    scores = compute_today_scores(db_session, test_user.id)
    assert scores["nutrition_score"] == 100
    assert scores["health_score"] == 100


def test_partial_progress_scales_linearly():
    assert calculate_sleep_score(4.0, target_hours=8.0) == 50
    assert calculate_hydration_score(1250.0, target_ml=2500.0) == 50
    assert calculate_activity_score(5000.0, target_steps=10000.0) == 50


def test_nutrition_score_penalises_overeating():
    """Перебор калорий снижает балл, но не обнуляет — иначе показатель бесполезен."""
    on_target = calculate_nutrition_score_calories(2000.0, 2000.0)
    over = calculate_nutrition_score_calories(3000.0, 2000.0)
    assert on_target == 100
    assert 40 <= over < 100
