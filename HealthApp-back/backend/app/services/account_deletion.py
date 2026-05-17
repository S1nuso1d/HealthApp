"""Полное удаление пользователя и связанных записей (без soft-delete)."""

from sqlalchemy.orm import Session

from app.services.avatar_storage import delete_avatar_file

from app.models.action_plan import ActionPlan
from app.models.activity import ActivityRecord
from app.models.health_sample import HealthSample
from app.models.analysis_run import AnalysisRun
from app.models.daily_health_summary import DailyHealthSummary
from app.models.hydration import HydrationRecord
from app.models.insight import Insight
from app.models.integration_credential import IntegrationCredential
from app.models.meal import MealRecord
from app.models.saved_dish import SavedDish
from app.models.profile import UserProfile
from app.models.saved_recommendation import SavedRecommendation
from app.models.sleep import SleepRecord
from app.models.smart_reminder import SmartReminder
from app.models.smart_trigger import SmartTrigger
from app.models.user import User
from app.models.user_state import UserState


def delete_user_and_related_data(db: Session, user_id: int) -> None:
    """Удаляет все строки, ссылающиеся на пользователя, затем самого пользователя."""
    delete_avatar_file(user_id)

    db.query(SmartReminder).filter(SmartReminder.user_id == user_id).delete()
    db.query(SmartTrigger).filter(SmartTrigger.user_id == user_id).delete()

    db.query(SavedRecommendation).filter(SavedRecommendation.user_id == user_id).delete()
    db.query(AnalysisRun).filter(AnalysisRun.user_id == user_id).delete()

    db.query(Insight).filter(Insight.user_id == user_id).delete()
    db.query(DailyHealthSummary).filter(DailyHealthSummary.user_id == user_id).delete()
    db.query(UserState).filter(UserState.user_id == user_id).delete()
    db.query(ActionPlan).filter(ActionPlan.user_id == user_id).delete()

    db.query(SleepRecord).filter(SleepRecord.user_id == user_id).delete()
    db.query(MealRecord).filter(MealRecord.user_id == user_id).delete()
    db.query(SavedDish).filter(SavedDish.user_id == user_id).delete()
    db.query(HydrationRecord).filter(HydrationRecord.user_id == user_id).delete()
    db.query(ActivityRecord).filter(ActivityRecord.user_id == user_id).delete()
    db.query(HealthSample).filter(HealthSample.user_id == user_id).delete()

    db.query(IntegrationCredential).filter(IntegrationCredential.user_id == user_id).delete()
    db.query(UserProfile).filter(UserProfile.user_id == user_id).delete()

    db.query(User).filter(User.id == user_id).delete()
    db.commit()
