"""Полное удаление пользователя и связанных записей (без soft-delete)."""

from __future__ import annotations

import re

from sqlalchemy.orm import Session

from app.core.config import settings
from app.services.avatar_storage import delete_avatar_file

from app.models.action_plan import ActionPlan
from app.models.activity import ActivityRecord
from app.models.analysis_run import AnalysisRun
from app.models.daily_health_summary import DailyHealthSummary
from app.models.gamification import UserAchievement
from app.models.habit_experiment import HabitExperiment
from app.models.health import CycleEntry, PillReminder
from app.models.health_sample import HealthSample
from app.models.hydration import HydrationRecord
from app.models.insight import Insight
from app.models.integration_credential import IntegrationCredential
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.refresh_token import RevokedRefreshToken
from app.models.saved_dish import SavedDish
from app.models.saved_recommendation import SavedRecommendation
from app.models.sleep import SleepRecord
from app.models.smart_reminder import SmartReminder
from app.models.smart_trigger import SmartTrigger
from app.models.social import (
    Challenge,
    ChallengeParticipant,
    Club,
    ClubMember,
    ClubNotification,
    ClubPost,
    FeedComment,
    FeedPost,
    FeedReaction,
    FeedStory,
    FeedStoryView,
    Friendship,
    UserBlock,
    UserPrivacySettings,
)
from app.models.user import User
from app.models.user_state import UserState

_MEDIA_FILE_RE = re.compile(r"/social/feed/media/([a-f0-9]{32}\.(?:jpeg|png|webp))$")


def _delete_media_file(media_url: str | None) -> None:
    if not media_url:
        return
    match = _MEDIA_FILE_RE.search(media_url)
    if not match:
        return
    path = settings.AVATAR_DIR_PATH / match.group(1)
    if path.is_file():
        path.unlink()


def delete_user_and_related_data(db: Session, user_id: int) -> None:
    """Удаляет все строки, ссылающиеся на пользователя, затем самого пользователя."""
    delete_avatar_file(user_id)

    for (url,) in db.query(FeedPost.media_url).filter(FeedPost.user_id == user_id).all():
        _delete_media_file(url)
    for (url,) in db.query(FeedStory.media_url).filter(FeedStory.user_id == user_id).all():
        _delete_media_file(url)

    db.query(FeedReaction).filter(FeedReaction.user_id == user_id).delete(synchronize_session=False)
    db.query(FeedComment).filter(FeedComment.user_id == user_id).delete(synchronize_session=False)
    db.query(FeedStoryView).filter(FeedStoryView.viewer_id == user_id).delete(synchronize_session=False)

    story_ids = [row[0] for row in db.query(FeedStory.id).filter(FeedStory.user_id == user_id).all()]
    if story_ids:
        db.query(FeedStoryView).filter(FeedStoryView.story_id.in_(story_ids)).delete(synchronize_session=False)
    db.query(FeedStory).filter(FeedStory.user_id == user_id).delete(synchronize_session=False)

    post_ids = [row[0] for row in db.query(FeedPost.id).filter(FeedPost.user_id == user_id).all()]
    if post_ids:
        db.query(FeedReaction).filter(FeedReaction.post_id.in_(post_ids)).delete(synchronize_session=False)
        db.query(FeedComment).filter(FeedComment.post_id.in_(post_ids)).delete(synchronize_session=False)
    db.query(FeedPost).filter(FeedPost.user_id == user_id).delete(synchronize_session=False)

    db.query(ClubNotification).filter(ClubNotification.user_id == user_id).delete(synchronize_session=False)
    db.query(ClubPost).filter(ClubPost.user_id == user_id).delete(synchronize_session=False)
    db.query(ClubMember).filter(ClubMember.user_id == user_id).delete(synchronize_session=False)
    owned_clubs = [row[0] for row in db.query(Club.id).filter(Club.creator_id == user_id).all()]
    if owned_clubs:
        db.query(ClubNotification).filter(ClubNotification.club_id.in_(owned_clubs)).delete(
            synchronize_session=False
        )
        db.query(ClubPost).filter(ClubPost.club_id.in_(owned_clubs)).delete(synchronize_session=False)
        db.query(ClubMember).filter(ClubMember.club_id.in_(owned_clubs)).delete(synchronize_session=False)
        db.query(Club).filter(Club.id.in_(owned_clubs)).delete(synchronize_session=False)

    db.query(ChallengeParticipant).filter(ChallengeParticipant.user_id == user_id).delete(
        synchronize_session=False
    )
    owned_challenges = [row[0] for row in db.query(Challenge.id).filter(Challenge.creator_id == user_id).all()]
    if owned_challenges:
        db.query(ChallengeParticipant).filter(
            ChallengeParticipant.challenge_id.in_(owned_challenges)
        ).delete(synchronize_session=False)
        db.query(Challenge).filter(Challenge.id.in_(owned_challenges)).delete(synchronize_session=False)

    db.query(Friendship).filter(
        (Friendship.requester_id == user_id) | (Friendship.addressee_id == user_id)
    ).delete(synchronize_session=False)
    db.query(UserBlock).filter(
        (UserBlock.blocker_id == user_id) | (UserBlock.blocked_id == user_id)
    ).delete(synchronize_session=False)
    db.query(UserPrivacySettings).filter(UserPrivacySettings.user_id == user_id).delete(
        synchronize_session=False
    )

    db.query(UserAchievement).filter(UserAchievement.user_id == user_id).delete(synchronize_session=False)
    db.query(HabitExperiment).filter(HabitExperiment.user_id == user_id).delete(synchronize_session=False)
    db.query(PillReminder).filter(PillReminder.user_id == user_id).delete(synchronize_session=False)
    db.query(CycleEntry).filter(CycleEntry.user_id == user_id).delete(synchronize_session=False)

    db.query(SmartReminder).filter(SmartReminder.user_id == user_id).delete(synchronize_session=False)
    db.query(SmartTrigger).filter(SmartTrigger.user_id == user_id).delete(synchronize_session=False)

    db.query(SavedRecommendation).filter(SavedRecommendation.user_id == user_id).delete(
        synchronize_session=False
    )
    db.query(AnalysisRun).filter(AnalysisRun.user_id == user_id).delete(synchronize_session=False)

    db.query(Insight).filter(Insight.user_id == user_id).delete(synchronize_session=False)
    db.query(DailyHealthSummary).filter(DailyHealthSummary.user_id == user_id).delete(
        synchronize_session=False
    )
    db.query(UserState).filter(UserState.user_id == user_id).delete(synchronize_session=False)
    db.query(ActionPlan).filter(ActionPlan.user_id == user_id).delete(synchronize_session=False)

    db.query(SleepRecord).filter(SleepRecord.user_id == user_id).delete(synchronize_session=False)
    db.query(MealRecord).filter(MealRecord.user_id == user_id).delete(synchronize_session=False)
    db.query(SavedDish).filter(SavedDish.user_id == user_id).delete(synchronize_session=False)
    db.query(HydrationRecord).filter(HydrationRecord.user_id == user_id).delete(synchronize_session=False)
    db.query(ActivityRecord).filter(ActivityRecord.user_id == user_id).delete(synchronize_session=False)
    db.query(HealthSample).filter(HealthSample.user_id == user_id).delete(synchronize_session=False)

    db.query(IntegrationCredential).filter(IntegrationCredential.user_id == user_id).delete(
        synchronize_session=False
    )
    db.query(UserProfile).filter(UserProfile.user_id == user_id).delete(synchronize_session=False)
    db.query(RevokedRefreshToken).filter(RevokedRefreshToken.user_id == user_id).delete(
        synchronize_session=False
    )

    db.query(User).filter(User.id == user_id).delete(synchronize_session=False)
    db.commit()
