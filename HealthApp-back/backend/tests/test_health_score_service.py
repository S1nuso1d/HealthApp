from app.services.health_score_service import compute_today_scores
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.models.hydration import HydrationRecord
from app.models.activity import ActivityRecord
from app.models.meal import MealRecord
from app.models.user_state import UserState
from datetime import datetime, timezone, timedelta

def test_compute_today_scores_empty_db(db_session, test_user):
    scores = compute_today_scores(db_session, test_user.id)
    assert scores["sleep_score"] == 0
    assert scores["hydration_score"] == 0
    assert scores["activity_score"] == 0
    assert scores["nutrition_score"] == 90 # default for 0 caffeine
    assert scores["state_score"] == 50 # default
    assert scores["health_score"] > 0 # Some base value

def test_compute_today_scores_with_data(db_session, test_user):
    now = datetime.now(timezone.utc)
    
    # Sleep
    sleep = SleepRecord(user_id=test_user.id, sleep_start=now - timedelta(hours=8), sleep_end=now, duration_hours=8.0)
    db_session.add(sleep)
    
    # Hydration
    water = HydrationRecord(user_id=test_user.id, amount_ml=2500, record_time=now)
    db_session.add(water)
    
    # Activity
    activity = ActivityRecord(user_id=test_user.id, activity_type="run", steps=10000, start_time=now)
    db_session.add(activity)
    
    db_session.commit()
    
    scores = compute_today_scores(db_session, test_user.id)
    assert scores["sleep_score"] == 100
    assert scores["hydration_score"] == 100
    assert scores["activity_score"] == 100
