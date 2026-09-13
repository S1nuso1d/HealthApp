from __future__ import annotations

import json
from datetime import datetime, timedelta, timezone

import anyio.to_thread
from fastapi import APIRouter, Depends, HTTPException, Query, File, UploadFile
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
from sqlalchemy import or_, func
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.rate_limit import upload_rate_limit
from app.db.database import get_db
from app.models.activity import ActivityRecord
from app.models.gamification import UserAchievement
from app.models.profile import UserProfile
from app.models.social import (
    FeedPost,
    FeedReaction,
    FeedComment,
    Friendship,
    UserPrivacySettings,
    Challenge,
    ChallengeParticipant,
    UserBlock,
    FeedStory,
    FeedStoryView,
    Club,
    ClubMember,
    ClubPost,
    ClubNotification,
)
from app.models.user import User
from app.services.profile_display import public_display_name
from app.services.user_search import user_matches_search_query
router = APIRouter(prefix="/social", tags=["Social"])


def _as_utc_aware(dt: datetime | None) -> datetime | None:
    if dt is None:
        return None
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt.astimezone(timezone.utc)


class PrivacyUpdate(BaseModel):
    profile_visibility: str = Field(default="friends", pattern="^(public|friends|private)$")
    feed_visibility: str = Field(default="friends", pattern="^(public|friends|private)$")
    show_activity_to_friends: bool = True
    show_achievements_to_friends: bool = True


class FriendRequestBody(BaseModel):
    user_id: int


class FeedPostCreate(BaseModel):
    body: str | None = None
    media_url: str | None = None
    media_type: str | None = Field(default=None, pattern="^(image|video)$")
    activity_id: int | None = None
    visibility: str = Field(default="friends", pattern="^(public|friends|private)$")


class FeedReactionBody(BaseModel):
    emoji: str = Field(..., min_length=1, max_length=16)


class FeedCommentCreate(BaseModel):
    text: str = Field(..., min_length=1, max_length=1000)


ALLOWED_REACTIONS = frozenset({"👍", "❤️", "🔥", "👏", "😊"})

class ChallengeCreate(BaseModel):
    title: str = Field(min_length=1, max_length=128)
    description: str | None = None
    challenge_type: str = Field(pattern="^(steps|calories|water)$")
    target_value: int = Field(gt=0)
    is_group: bool = False
    duration_days: int = Field(default=7, gt=0, le=30)

class ChallengeResponse(BaseModel):
    id: int
    title: str
    description: str | None
    challenge_type: str
    target_value: int
    is_group: bool
    creator_id: int
    start_date: str
    end_date: str
    participants_count: int
    my_progress: int | None = None


class ClubCreate(BaseModel):
    name: str = Field(min_length=2, max_length=128)
    description: str | None = None
    avatar_url: str | None = None
    rules: str | None = None


class ClubResponse(BaseModel):
    id: int
    name: str
    description: str | None
    avatar_url: str | None
    rules: str | None
    creator_id: int
    members_count: int
    is_member: bool


class ClubMemberResponse(BaseModel):
    id: int
    club_id: int
    user: dict
    role: str
    joined_at: str


class ClubUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=2, max_length=128)
    description: str | None = None
    avatar_url: str | None = None
    rules: str | None = None


class ClubMemberRoleUpdate(BaseModel):
    role: str = Field(pattern="^(admin|member)$")


class ClubNotificationResponse(BaseModel):
    id: int
    club_id: int
    event_type: str
    title: str
    message: str
    club_name: str
    created_at: str


class ClubPostCreate(BaseModel):
    post_type: str = Field(default="discussion", pattern="^(discussion|achievement|poll)$")
    body: str | None = None
    poll_options: list[str] | None = None


class ClubPostResponse(BaseModel):
    id: int
    club_id: int
    user: dict
    post_type: str
    body: str | None
    poll_options: list[str] | None = None
    poll_votes: dict[str, int] | None = None
    my_vote: str | None = None
    created_at: str


class ClubPollVote(BaseModel):
    option: str


def _privacy(db: Session, user_id: int) -> UserPrivacySettings:
    row = db.query(UserPrivacySettings).filter(UserPrivacySettings.user_id == user_id).first()
    if not row:
        row = UserPrivacySettings(user_id=user_id)
        db.add(row)
        db.commit()
        db.refresh(row)
    return row


def _are_friends(db: Session, a: int, b: int) -> bool:
    if a == b:
        return True
    return (
        db.query(Friendship)
        .filter(
            Friendship.status == "accepted",
            or_(
                (Friendship.requester_id == a) & (Friendship.addressee_id == b),
                (Friendship.requester_id == b) & (Friendship.addressee_id == a),
            ),
        )
        .first()
        is not None
    )


def _is_blocked(db: Session, user_a: int, user_b: int) -> bool:
    if user_a == user_b:
        return False
    return (
        db.query(UserBlock)
        .filter(
            or_(
                (UserBlock.blocker_id == user_a) & (UserBlock.blocked_id == user_b),
                (UserBlock.blocker_id == user_b) & (UserBlock.blocked_id == user_a),
            )
        )
        .first()
        is not None
    )

def _share_club(db: Session, a: int, b: int) -> bool:
    if a == b:
        return True
    club_ids = [
        row[0]
        for row in db.query(ClubMember.club_id).filter(ClubMember.user_id == a).all()
    ]
    if not club_ids:
        return False
    return (
        db.query(ClubMember)
        .filter(ClubMember.user_id == b, ClubMember.club_id.in_(club_ids))
        .first()
        is not None
    )


def _can_view_profile(db: Session, viewer_id: int, target_id: int) -> bool:
    if viewer_id == target_id:
        return True
    if _is_blocked(db, viewer_id, target_id):
        return False
    p = _privacy(db, target_id)
    if p.profile_visibility == "public":
        return True
    if p.profile_visibility == "private":
        return False
    return _are_friends(db, viewer_id, target_id) or _share_club(db, viewer_id, target_id)


def _can_view_feed(db: Session, viewer_id: int, author_id: int, post_visibility: str) -> bool:
    if viewer_id == author_id:
        return True
    if _is_blocked(db, viewer_id, author_id):
        return False
    vis = post_visibility or "friends"
    if vis == "public":
        return True
    if vis == "private":
        return False
    privacy = _privacy(db, author_id)
    if privacy.feed_visibility == "private":
        return False
    if privacy.feed_visibility == "public":
        return True
    return _are_friends(db, viewer_id, author_id) or _share_club(db, viewer_id, author_id)


def _activity_payload(a: ActivityRecord) -> dict:
    return {
        "id": a.id,
        "activity_type": a.activity_type,
        "duration_minutes": a.duration_minutes,
        "calories_burned": float(a.calories_burned or 0),
        "steps": a.steps,
        "distance_km": float(a.distance_km) if a.distance_km is not None else None,
        "start_time": a.start_time.isoformat() if a.start_time else None,
    }


def _reactions_for_posts(db: Session, post_ids: list[int], viewer_id: int) -> dict[int, dict]:
    if not post_ids:
        return {}
    rows = db.query(FeedReaction).filter(FeedReaction.post_id.in_(post_ids)).all()
    by_post: dict[int, list[FeedReaction]] = {}
    for r in rows:
        by_post.setdefault(r.post_id, []).append(r)
    out: dict[int, dict] = {}
    for pid, reacts in by_post.items():
        counts: dict[str, int] = {}
        my_reaction = None
        for r in reacts:
            counts[r.emoji] = counts.get(r.emoji, 0) + 1
            if r.user_id == viewer_id:
                my_reaction = r.emoji
        out[pid] = {
            "counts": [{"emoji": e, "count": c} for e, c in sorted(counts.items(), key=lambda x: -x[1])],
            "total": sum(counts.values()),
            "my_reaction": my_reaction,
        }
    return out


class PostContext:
    """Данные для пачки постов, собранные заранее.

    Без этого сериализация одной ленты из 40 постов делала около 160 запросов:
    автор, профиль автора, тренировка и счётчик комментариев на каждый пост.
    """

    __slots__ = ("profiles", "activities", "comment_counts")

    def __init__(
        self,
        profiles: dict[int, UserProfile],
        activities: dict[int, ActivityRecord],
        comment_counts: dict[int, int],
    ) -> None:
        self.profiles = profiles
        self.activities = activities
        self.comment_counts = comment_counts


def _prefetch_post_context(db: Session, posts: list[FeedPost]) -> PostContext:
    user_ids = {p.user_id for p in posts if p.user_id}
    activity_ids = {p.activity_id for p in posts if p.activity_id}
    post_ids = [p.id for p in posts]

    profiles: dict[int, UserProfile] = {}
    if user_ids:
        profiles = {
            row.user_id: row
            for row in db.query(UserProfile)
            .filter(UserProfile.user_id.in_(user_ids))
            .all()
        }

    activities: dict[int, ActivityRecord] = {}
    if activity_ids:
        activities = {
            row.id: row
            for row in db.query(ActivityRecord)
            .filter(ActivityRecord.id.in_(activity_ids))
            .all()
        }

    comment_counts: dict[int, int] = {}
    if post_ids:
        comment_counts = dict(
            db.query(FeedComment.post_id, func.count(FeedComment.id))
            .filter(FeedComment.post_id.in_(post_ids))
            .group_by(FeedComment.post_id)
            .all()
        )

    return PostContext(profiles, activities, comment_counts)


def _users_by_ids(db: Session, user_ids: set[int]) -> dict[int, User]:
    if not user_ids:
        return {}
    return {row.id: row for row in db.query(User).filter(User.id.in_(user_ids)).all()}


def _serialize_post(
    db: Session,
    p: FeedPost,
    author: User,
    viewer_id: int,
    reaction_meta: dict | None = None,
    context: PostContext | None = None,
) -> dict:
    act = None
    if p.activity_id:
        if context is not None:
            a = context.activities.get(p.activity_id)
        else:
            a = db.query(ActivityRecord).filter(ActivityRecord.id == p.activity_id).first()
        if a:
            act = _activity_payload(a)
    meta = reaction_meta or {"counts": [], "total": 0, "my_reaction": None}
    if context is not None:
        comments_count = context.comment_counts.get(p.id, 0)
    else:
        comments_count = db.query(FeedComment).filter(FeedComment.post_id == p.id).count()
    return {
        "id": p.id,
        "author": _user_card(
            db,
            author,
            viewer_id,
            profiles=context.profiles if context is not None else None,
        ),
        "body": p.body,
        "media_url": p.media_url,
        "media_type": p.media_type,
        "activity": act,
        "activity_id": p.activity_id,
        "created_at": p.created_at.isoformat() if p.created_at else None,
        "reactions": meta["counts"],
        "reaction_total": meta["total"],
        "my_reaction": meta["my_reaction"],
        "comments_count": comments_count,
    }


def _user_card(
    db: Session,
    user: User,
    viewer_id: int,
    profiles: dict[int, UserProfile] | None = None,
) -> dict:
    if profiles is not None:
        profile = profiles.get(user.id)
    else:
        profile = db.query(UserProfile).filter(UserProfile.user_id == user.id).first()
    nick = (profile.nickname or "").strip() if profile else ""
    return {
        "user_id": user.id,
        "display_name": public_display_name(profile, user),
        "nickname": nick or None,
        "first_name": (profile.first_name or "").strip() or None if profile else None,
        "last_name": (profile.last_name or "").strip() or None if profile else None,
        "goal": profile.goal if profile else None,
        "age": profile.age if profile and profile.age and profile.age > 0 else None,
        "has_avatar": bool(profile and profile.has_avatar),
        "is_self": user.id == viewer_id,
    }


@router.get("/privacy")
def get_privacy(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    p = _privacy(db, current_user.id)
    return {
        "profile_visibility": p.profile_visibility,
        "feed_visibility": p.feed_visibility,
        "show_activity_to_friends": p.show_activity_to_friends,
        "show_achievements_to_friends": p.show_achievements_to_friends,
    }


@router.put("/privacy")
def update_privacy(
    body: PrivacyUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    p = _privacy(db, current_user.id)
    p.profile_visibility = body.profile_visibility
    p.feed_visibility = body.feed_visibility
    p.show_activity_to_friends = body.show_activity_to_friends
    p.show_achievements_to_friends = body.show_achievements_to_friends
    db.commit()
    return get_privacy(current_user=current_user, db=db)


@router.get("/users/search")
def search_users(
    q: str = Query(..., min_length=2, max_length=64),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    rows = (
        db.query(User, UserProfile)
        .outerjoin(UserProfile, UserProfile.user_id == User.id)
        .filter(User.id != current_user.id)
        .limit(500)
        .all()
    )
    matched: list[User] = []
    for user, profile in rows:
        if _is_blocked(db, current_user.id, user.id):
            continue
        if user_matches_search_query(user, profile, q):
            matched.append(user)
    return {"users": [_user_card(db, u, current_user.id) for u in matched[:20]]}


@router.get("/friends")
def list_friends(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    rows = (
        db.query(Friendship)
        .filter(
            Friendship.status == "accepted",
            or_(
                Friendship.requester_id == current_user.id,
                Friendship.addressee_id == current_user.id,
            ),
        )
        .all()
    )
    friends = []
    for f in rows:
        fid = f.addressee_id if f.requester_id == current_user.id else f.requester_id
        if _is_blocked(db, current_user.id, fid):
            continue
        u = db.query(User).filter(User.id == fid).first()
        if u:
            friends.append(_user_card(db, u, current_user.id))
    return {"friends": friends}


@router.get("/friends/pending")
def pending_requests(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    incoming = (
        db.query(Friendship)
        .filter(Friendship.addressee_id == current_user.id, Friendship.status == "pending")
        .all()
    )
    out = []
    for f in incoming:
        if _is_blocked(db, current_user.id, f.requester_id):
            continue
        u = db.query(User).filter(User.id == f.requester_id).first()
        if u:
            out.append({**_user_card(db, u, current_user.id), "friendship_id": f.id})
    return {"incoming": out}


@router.post("/friends/request")
def request_friend(
    body: FriendRequestBody,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.user_id == current_user.id:
        raise HTTPException(400, detail="Нельзя добавить себя")
    if _is_blocked(db, current_user.id, body.user_id):
        raise HTTPException(403, detail="Действие недоступно")
    target = db.query(User).filter(User.id == body.user_id).first()
    if not target:
        raise HTTPException(404, detail="Пользователь не найден")
    existing = (
        db.query(Friendship)
        .filter(
            or_(
                (Friendship.requester_id == current_user.id) & (Friendship.addressee_id == body.user_id),
                (Friendship.requester_id == body.user_id) & (Friendship.addressee_id == current_user.id),
            ),
        )
        .first()
    )
    if existing:
        if existing.status == "accepted":
            return {"status": "already_friends"}
        if existing.status == "pending":
            return {"status": "pending"}
        raise HTTPException(400, detail="Заявка недоступна")
    row = Friendship(requester_id=current_user.id, addressee_id=body.user_id, status="pending")
    db.add(row)
    db.commit()
    return {"status": "requested", "friendship_id": row.id}


@router.post("/friends/{friendship_id}/accept")
def accept_friend(
    friendship_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    row = db.query(Friendship).filter(Friendship.id == friendship_id).first()
    if not row or row.addressee_id != current_user.id or row.status != "pending":
        raise HTTPException(404, detail="Заявка не найдена")
    row.status = "accepted"
    db.commit()
    return {"status": "accepted"}


@router.post("/friends/{friendship_id}/decline")
def decline_friend(
    friendship_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    row = db.query(Friendship).filter(Friendship.id == friendship_id).first()
    if not row or row.addressee_id != current_user.id or row.status != "pending":
        raise HTTPException(404, detail="Заявка не найдена")
    db.delete(row)
    db.commit()
    return {"status": "declined"}


@router.delete("/friends/{user_id}")
def remove_friend(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    row = (
        db.query(Friendship)
        .filter(
            Friendship.status == "accepted",
            or_(
                (Friendship.requester_id == current_user.id) & (Friendship.addressee_id == user_id),
                (Friendship.requester_id == user_id) & (Friendship.addressee_id == current_user.id),
            ),
        )
        .first()
    )
    if not row:
        raise HTTPException(404, detail="Дружба не найдена")
    db.delete(row)
    db.commit()
    return {"status": "removed"}


@router.post(
    "/feed/media",
    summary="Загрузить фото для публикации",
    dependencies=[Depends(upload_rate_limit)],
)
async def upload_post_media(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    import uuid
    from app.services.avatar_storage import validate_avatar_bytes
    from app.core.config import settings

    content = await file.read()
    ok, detail = validate_avatar_bytes(content)
    if not ok:
        raise HTTPException(status_code=400, detail=detail)
    fmt = detail
    filename = f"{uuid.uuid4().hex}.{fmt}"
    path = settings.AVATAR_DIR_PATH / filename
    settings.AVATAR_DIR_PATH.mkdir(parents=True, exist_ok=True)
    await anyio.to_thread.run_sync(path.write_bytes, content)

    # Returns relative URL which frontend can prepend with base URL
    return {"url": f"/social/feed/media/{filename}"}


@router.get("/feed/media/{filename}")
def get_post_media(
    filename: str,
    current_user: User = Depends(get_current_user),
):
    """Требует авторизации: имя файла — случайный UUID, но это не средство защиты."""
    import re
    from app.services.avatar_storage import guess_media_type
    from app.core.config import settings

    if not re.match(r"^[a-f0-9]{32}\.(jpeg|png|webp)$", filename):
        raise HTTPException(400, "Invalid filename")
    path = settings.AVATAR_DIR_PATH / filename
    if not path.is_file():
        raise HTTPException(404, "Not found")
    return FileResponse(path, media_type=guess_media_type(path))


@router.get("/feed")
def get_feed(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    posts = db.query(FeedPost).order_by(FeedPost.created_at.desc()).limit(80).all()
    authors = _users_by_ids(db, {p.user_id for p in posts if p.user_id})
    visible: list[tuple[FeedPost, User]] = []
    for p in posts:
        if not _can_view_feed(db, current_user.id, p.user_id, p.visibility):
            continue
        author = authors.get(p.user_id)
        if author:
            visible.append((p, author))
    visible = visible[:40]
    post_ids = [p.id for p, _ in visible]
    reactions_map = _reactions_for_posts(db, post_ids, current_user.id)
    context = _prefetch_post_context(db, [p for p, _ in visible])
    items = [
        _serialize_post(
            db,
            p,
            author,
            current_user.id,
            reactions_map.get(
                p.id,
                {"counts": [], "total": 0, "my_reaction": None},
            ),
            context=context,
        )
        for p, author in visible
    ]
    return {"posts": items}


class StoryCreate(BaseModel):
    media_url: str


def _story_view_counts(db: Session, story_ids: list[int], owner_id: int) -> dict[int, int]:
    if not story_ids:
        return {}
    rows = (
        db.query(FeedStoryView.story_id, func.count(FeedStoryView.id))
        .filter(FeedStoryView.story_id.in_(story_ids))
        .filter(FeedStoryView.viewer_id != owner_id)
        .group_by(FeedStoryView.story_id)
        .all()
    )
    return {story_id: int(count) for story_id, count in rows}


def _can_view_story(db: Session, viewer_id: int, story: FeedStory) -> bool:
    if story.user_id == viewer_id:
        return True
    return story.user_id in _friend_user_ids(db, viewer_id)


@router.post("/stories", summary="Создать историю")
def create_story(
    body: StoryCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from datetime import datetime, timedelta, timezone
    
    if not body.media_url:
        raise HTTPException(400, "media_url is required")
        
    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(hours=24)
    
    story = FeedStory(
        user_id=current_user.id,
        media_url=body.media_url,
        expires_at=expires_at
    )
    db.add(story)
    db.commit()
    db.refresh(story)
    
    return {"id": story.id, "media_url": story.media_url, "expires_at": story.expires_at.isoformat()}


@router.get("/stories")
def get_friend_stories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Кружки «историй»: активные истории друзей и свои."""
    from datetime import datetime, timezone

    now = datetime.now(timezone.utc)
    user_ids = [current_user.id] + _friend_user_ids(db, current_user.id)
    
    # Fetch all active stories for these users
    active_stories = (
        db.query(FeedStory)
        .filter(FeedStory.user_id.in_(user_ids))
        .order_by(FeedStory.created_at.asc())
        .all()
    )
    active_stories = [
        s for s in active_stories
        if _as_utc_aware(s.expires_at) and _as_utc_aware(s.expires_at) > now
    ]
    
    # Group by user
    by_user = {}
    for s in active_stories:
        by_user.setdefault(s.user_id, []).append(s)

    view_counts_by_story: dict[int, int] = {}
    for uid, user_stories in by_user.items():
        if uid == current_user.id:
            view_counts_by_story.update(_story_view_counts(db, [s.id for s in user_stories], uid))
        
    stories_response = []
    for uid, user_stories in by_user.items():
        author = db.query(User).filter(User.id == uid).first()
        if not author:
            continue
            
        # For preview, we can use the first active story or the last one
        preview_story = user_stories[-1]
            
        stories_response.append(
            {
                "author": _user_card(db, author, current_user.id),
                "preview_url": preview_story.media_url,
                "has_unseen": uid != current_user.id,
                "items": [
                    {
                        "id": s.id,
                        "media_url": s.media_url,
                        "created_at": s.created_at.isoformat() if s.created_at else None,
                        "expires_at": s.expires_at.isoformat() if s.expires_at else None,
                        "view_count": view_counts_by_story.get(s.id, 0) if uid == current_user.id else 0,
                    }
                    for s in user_stories
                ]
            }
        )
        
    return {"stories": stories_response}


@router.post("/stories/{story_id}/view", summary="Отметить просмотр истории")
def record_story_view(
    story_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    story = db.query(FeedStory).filter(FeedStory.id == story_id).first()
    if not story:
        raise HTTPException(404, detail="История не найдена")

    now = datetime.now(timezone.utc)
    if _as_utc_aware(story.expires_at) <= now:
        raise HTTPException(404, detail="История истекла")

    if not _can_view_story(db, current_user.id, story):
        raise HTTPException(403, detail="Нет доступа")

    if story.user_id != current_user.id:
        existing = (
            db.query(FeedStoryView)
            .filter(
                FeedStoryView.story_id == story_id,
                FeedStoryView.viewer_id == current_user.id,
            )
            .first()
        )
        if not existing:
            db.add(FeedStoryView(story_id=story_id, viewer_id=current_user.id))
            db.commit()

    view_count = _story_view_counts(db, [story.id], story.user_id).get(story.id, 0)
    return {"view_count": view_count}


@router.get("/activities/linkable")
def linkable_activities(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Тренировки пользователя для привязки к публикации (последние 14 дней)."""
    from datetime import datetime, timedelta, timezone

    since = datetime.now(timezone.utc) - timedelta(days=14)
    rows = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == current_user.id,
            ActivityRecord.start_time >= since,
        )
        .order_by(ActivityRecord.start_time.desc())
        .limit(30)
        .all()
    )
    return {"activities": [_activity_payload(a) for a in rows]}


@router.post("/feed/{post_id}/reactions")
def toggle_feed_reaction(
    post_id: int,
    body: FeedReactionBody,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.emoji not in ALLOWED_REACTIONS:
        raise HTTPException(400, detail="Недопустимая реакция")
    post = db.query(FeedPost).filter(FeedPost.id == post_id).first()
    if not post:
        raise HTTPException(404, detail="Публикация не найдена")
    if not _can_view_feed(db, current_user.id, post.user_id, post.visibility):
        raise HTTPException(403, detail="Нет доступа к публикации")
    existing = (
        db.query(FeedReaction)
        .filter(FeedReaction.post_id == post_id, FeedReaction.user_id == current_user.id)
        .first()
    )
    if existing and existing.emoji == body.emoji:
        db.delete(existing)
        db.commit()
        return {"status": "removed", "my_reaction": None}
    if existing:
        existing.emoji = body.emoji
        db.commit()
        return {"status": "updated", "my_reaction": body.emoji}
    db.add(FeedReaction(post_id=post_id, user_id=current_user.id, emoji=body.emoji))
    db.commit()
    return {"status": "added", "my_reaction": body.emoji}


@router.get("/feed/{post_id}/comments")
def get_post_comments(
    post_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    post = db.query(FeedPost).filter(FeedPost.id == post_id).first()
    if not post:
        raise HTTPException(404, detail="Публикация не найдена")
    if not _can_view_feed(db, current_user.id, post.user_id, post.visibility):
        raise HTTPException(403, detail="Нет доступа к публикации")
    
    comments = db.query(FeedComment).filter(FeedComment.post_id == post_id).order_by(FeedComment.created_at.asc()).all()
    results = []
    for c in comments:
        author = db.query(User).filter(User.id == c.user_id).first()
        results.append({
            "id": c.id,
            "post_id": c.post_id,
            "author": _user_card(db, author, current_user.id) if author else None,
            "text": c.text,
            "created_at": c.created_at.isoformat() if c.created_at else None,
        })
    return {"comments": results}

@router.post("/feed/{post_id}/comments")
def create_post_comment(
    post_id: int,
    body: FeedCommentCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    post = db.query(FeedPost).filter(FeedPost.id == post_id).first()
    if not post:
        raise HTTPException(404, detail="Публикация не найдена")
    if not _can_view_feed(db, current_user.id, post.user_id, post.visibility):
        raise HTTPException(403, detail="Нет доступа к публикации")
    
    comment = FeedComment(
        post_id=post_id,
        user_id=current_user.id,
        text=body.text,
    )
    db.add(comment)
    db.commit()
    db.refresh(comment)
    author = db.query(User).filter(User.id == current_user.id).first()
    
    return {
        "id": comment.id,
        "post_id": comment.post_id,
        "author": _user_card(db, author, current_user.id),
        "text": comment.text,
        "created_at": comment.created_at.isoformat() if comment.created_at else None,
    }


@router.post("/feed")
def create_post(
    body: FeedPostCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if body.activity_id is not None:
        act = (
            db.query(ActivityRecord)
            .filter(
                ActivityRecord.id == body.activity_id,
                ActivityRecord.user_id == current_user.id,
            )
            .first()
        )
        if not act:
            raise HTTPException(400, detail="Тренировка не найдена или принадлежит другому пользователю")
    if not (body.body or body.media_url or body.activity_id):
        raise HTTPException(400, detail="Добавьте текст, фото или привяжите тренировку")
    row = FeedPost(
        user_id=current_user.id,
        body=body.body,
        media_url=body.media_url,
        media_type=body.media_type,
        activity_id=body.activity_id,
        visibility=body.visibility,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return {"id": row.id, "status": "created"}


@router.delete("/feed/{post_id}")
def delete_post(
    post_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    post = db.query(FeedPost).filter(FeedPost.id == post_id).first()
    if not post:
        raise HTTPException(404, detail="Публикация не найдена")
    if post.user_id != current_user.id:
        raise HTTPException(403, detail="Можно удалять только свои публикации")
    db.delete(post)
    db.commit()
    return {"status": "deleted"}


def _week_start_utc():
    from datetime import datetime, timedelta, timezone

    now = datetime.now(timezone.utc)
    start = (now - timedelta(days=now.weekday())).replace(hour=0, minute=0, second=0, microsecond=0)
    return start, now


def _steps_between(db: Session, user_id: int, start, end) -> int:
    rows = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= start,
            ActivityRecord.start_time <= end,
        )
        .all()
    )
    return int(sum(int(r.steps or 0) for r in rows))


def _friend_user_ids(db: Session, user_id: int) -> list[int]:
    rows = (
        db.query(Friendship)
        .filter(
            Friendship.status == "accepted",
            or_(Friendship.requester_id == user_id, Friendship.addressee_id == user_id),
        )
        .all()
    )
    ids = []
    for f in rows:
        fid = f.addressee_id if f.requester_id == user_id else f.requester_id
        ids.append(fid)
    return ids


@router.get("/challenges/weekly")
def weekly_steps_challenge(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Лёгкий челлендж: шаги за текущую неделю среди друзей."""
    start, end = _week_start_utc()
    entries = [
        {
            "user_id": current_user.id,
            "display_name": "Вы",
            "steps": _steps_between(db, current_user.id, start, end),
            "is_me": True,
        }
    ]
    for fid in _friend_user_ids(db, current_user.id):
        friend = db.query(User).filter(User.id == fid).first()
        if not friend:
            continue
        privacy = _privacy(db, fid)
        if not privacy.show_activity_to_friends:
            continue
        card = _user_card(db, friend, current_user.id)
        entries.append(
            {
                "user_id": fid,
                "display_name": card["display_name"],
                "steps": _steps_between(db, fid, start, end),
                "is_me": False,
            }
        )
    entries.sort(key=lambda e: e["steps"], reverse=True)
    for i, e in enumerate(entries):
        e["rank"] = i + 1
    return {"metric": "steps", "period": "week", "entries": entries}


@router.get("/users/{user_id}/avatar", summary="Аватар пользователя")
def get_user_avatar(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.avatar_storage import find_existing_avatar_path, guess_media_type

    if user_id == current_user.id:
        can_view = True
    else:
        if db.query(UserBlock).filter_by(blocker_id=user_id, blocked_id=current_user.id).first():
            raise HTTPException(status_code=403, detail="Профиль закрыт настройками приватности")
        p = _privacy(db, user_id)
        can_view = (
            p.profile_visibility == "public"
            or (p.profile_visibility == "friends" and _are_friends(db, current_user.id, user_id))
        )
    if not can_view:
        raise HTTPException(status_code=403, detail="Профиль закрыт настройками приватности")

    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    if not profile or not profile.has_avatar:
        raise HTTPException(status_code=404, detail="Аватар не найден")
    path = find_existing_avatar_path(user_id)
    if not path:
        profile.has_avatar = False
        db.commit()
        raise HTTPException(status_code=404, detail="Аватар не найден")
    return FileResponse(path, media_type=guess_media_type(path), filename=path.name)


@router.get("/users/{user_id}/profile")
def friend_profile(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(404, detail="Пользователь не найден")

    is_blocked_by_target = db.query(UserBlock).filter_by(blocker_id=user_id, blocked_id=current_user.id).first() is not None
    has_blocked_target = db.query(UserBlock).filter_by(blocker_id=current_user.id, blocked_id=user_id).first() is not None

    if is_blocked_by_target:
        raise HTTPException(403, detail="Профиль закрыт настройками приватности")

    card = _user_card(db, user, current_user.id)
    activities = []
    achievements = []
    posts = []

    if not has_blocked_target:
        can_view = _can_view_profile(db, current_user.id, user_id)

        if not can_view:
            raise HTTPException(403, detail="Профиль закрыт настройками приватности")

        p = _privacy(db, user_id)
        can_see_details = (
            current_user.id == user_id
            or _are_friends(db, current_user.id, user_id)
            or _share_club(db, current_user.id, user_id)
        )
        if p.show_activity_to_friends and can_see_details:
            acts = (
                db.query(ActivityRecord)
                .filter(ActivityRecord.user_id == user_id)
                .order_by(ActivityRecord.start_time.desc())
                .limit(12)
                .all()
            )
            activities = [
                {
                    "id": a.id,
                    "activity_type": a.activity_type,
                    "duration_minutes": a.duration_minutes,
                    "calories_burned": float(a.calories_burned or 0),
                    "steps": a.steps,
                    "start_time": a.start_time.isoformat() if a.start_time else None,
                }
                for a in acts
            ]
        if p.show_achievements_to_friends and can_see_details:
            unlocked = (
                db.query(UserAchievement)
                .filter(UserAchievement.user_id == user_id)
                .order_by(UserAchievement.unlocked_at.desc())
                .limit(12)
                .all()
            )
            achievements = [
                {
                    "code": u.achievement_code,
                    "title": u.title,
                    "icon_key": u.icon_key,
                    "points": u.points,
                    "unlocked_at": u.unlocked_at.isoformat() if u.unlocked_at else None,
                }
                for u in unlocked
            ]

        feed_posts = (
            db.query(FeedPost)
            .filter(FeedPost.user_id == user_id)
            .order_by(FeedPost.created_at.desc())
            .limit(30)
            .all()
        )
        visible_posts: list[FeedPost] = [
            p
            for p in feed_posts
            if _can_view_feed(db, current_user.id, p.user_id, p.visibility)
        ]
        post_ids = [p.id for p in visible_posts]
        reactions_map = _reactions_for_posts(db, post_ids, current_user.id)
        shown_posts = visible_posts[:20]
        context = _prefetch_post_context(db, shown_posts)
        posts = [
            _serialize_post(
                db,
                p,
                user,
                current_user.id,
                reactions_map.get(p.id, {"counts": [], "total": 0, "my_reaction": None}),
                context=context,
            )
            for p in shown_posts
        ]
    return {
        "user": card,
        "activities": activities,
        "achievements": achievements,
        "posts": posts,
        "is_friend": _are_friends(db, current_user.id, user_id) if not has_blocked_target else False,
        "is_blocked": has_blocked_target,
    }


@router.post("/users/{user_id}/block")
def block_user(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if user_id == current_user.id:
        raise HTTPException(400, detail="Нельзя заблокировать себя")
    
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(404, detail="Пользователь не найден")
        
    existing = db.query(UserBlock).filter_by(blocker_id=current_user.id, blocked_id=user_id).first()
    if existing:
        return {"status": "already_blocked"}
        
    block = UserBlock(blocker_id=current_user.id, blocked_id=user_id)
    db.add(block)
    
    # Remove friendship if exists
    friendship = db.query(Friendship).filter(
        or_(
            (Friendship.requester_id == current_user.id) & (Friendship.addressee_id == user_id),
            (Friendship.requester_id == user_id) & (Friendship.addressee_id == current_user.id),
        )
    ).first()
    if friendship:
        db.delete(friendship)
        
    db.commit()
    return {"status": "blocked"}


@router.delete("/users/{user_id}/block")
def unblock_user(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    block = db.query(UserBlock).filter_by(blocker_id=current_user.id, blocked_id=user_id).first()
    if not block:
        raise HTTPException(404, detail="Пользователь не заблокирован")
        
    db.delete(block)
    db.commit()
    return {"status": "unblocked"}


@router.post("/challenges", response_model=ChallengeResponse, summary="Создать челлендж")
def create_challenge(
    payload: ChallengeCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from datetime import datetime, timezone, timedelta
    now = datetime.now(timezone.utc)
    challenge = Challenge(
        title=payload.title,
        description=payload.description,
        challenge_type=payload.challenge_type,
        target_value=payload.target_value,
        is_group=payload.is_group,
        creator_id=current_user.id,
        start_date=now,
        end_date=now + timedelta(days=payload.duration_days)
    )
    db.add(challenge)
    db.commit()
    db.refresh(challenge)
    
    # Creator automatically joins
    participant = ChallengeParticipant(challenge_id=challenge.id, user_id=current_user.id)
    db.add(participant)
    db.commit()
    
    return ChallengeResponse(
        id=challenge.id,
        title=challenge.title,
        description=challenge.description,
        challenge_type=challenge.challenge_type,
        target_value=challenge.target_value,
        is_group=challenge.is_group,
        creator_id=challenge.creator_id,
        start_date=challenge.start_date.isoformat(),
        end_date=challenge.end_date.isoformat(),
        participants_count=1,
        my_progress=0
    )

@router.get("/challenges", response_model=list[ChallengeResponse], summary="Получить доступные и активные челленджи")
def get_challenges(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    challenges = db.query(Challenge).all()
    # Два агрегирующих запроса вместо двух запросов на каждый челлендж
    counts = dict(
        db.query(ChallengeParticipant.challenge_id, func.count(ChallengeParticipant.id))
        .group_by(ChallengeParticipant.challenge_id)
        .all()
    )
    my_participation = {
        row.challenge_id: row
        for row in db.query(ChallengeParticipant)
        .filter(ChallengeParticipant.user_id == current_user.id)
        .all()
    }
    results = []
    for c in challenges:
        p_count = counts.get(c.id, 0)
        me = my_participation.get(c.id)
        results.append(
            ChallengeResponse(
                id=c.id,
                title=c.title,
                description=c.description,
                challenge_type=c.challenge_type,
                target_value=c.target_value,
                is_group=c.is_group,
                creator_id=c.creator_id,
                start_date=c.start_date.isoformat(),
                end_date=c.end_date.isoformat(),
                participants_count=p_count,
                my_progress=me.progress if me else None
            )
        )
    return results

@router.post("/challenges/{challenge_id}/join", summary="Присоединиться к челленджу")
def join_challenge(
    challenge_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.query(Challenge).filter(Challenge.id == challenge_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Челлендж не найден")
    
    existing = db.query(ChallengeParticipant).filter(
        ChallengeParticipant.challenge_id == challenge_id, 
        ChallengeParticipant.user_id == current_user.id
    ).first()
    
    if existing:
        return {"status": "ok", "message": "Вы уже участвуете"}
        
    p = ChallengeParticipant(challenge_id=challenge_id, user_id=current_user.id)
    db.add(p)
    db.commit()
    return {"status": "ok"}


@router.get("/clubs", response_model=list[ClubResponse], summary="Получить список клубов")
def get_clubs(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    clubs = db.query(Club).all()
    counts = dict(
        db.query(ClubMember.club_id, func.count(ClubMember.id))
        .group_by(ClubMember.club_id)
        .all()
    )
    my_memberships = {
        row.club_id
        for row in db.query(ClubMember)
        .filter(ClubMember.user_id == current_user.id)
        .all()
    }
    results = []
    for c in clubs:
        m_count = counts.get(c.id, 0)
        me = c.id in my_memberships
        results.append(
            ClubResponse(
                id=c.id,
                name=c.name,
                description=c.description,
                avatar_url=c.avatar_url,
                rules=c.rules,
                creator_id=c.creator_id,
                members_count=m_count,
                is_member=me,
            )
        )
    return results


@router.post("/clubs", response_model=ClubResponse, summary="Создать клуб")
def create_club(
    payload: ClubCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    club = Club(
        name=payload.name,
        description=payload.description,
        avatar_url=payload.avatar_url,
        rules=payload.rules,
        creator_id=current_user.id
    )
    db.add(club)
    db.commit()
    db.refresh(club)

    member = ClubMember(club_id=club.id, user_id=current_user.id, role="admin")
    db.add(member)
    db.commit()

    return ClubResponse(
        id=club.id,
        name=club.name,
        description=club.description,
        avatar_url=club.avatar_url,
        rules=club.rules,
        creator_id=club.creator_id,
        members_count=1,
        is_member=True
    )


@router.get("/clubs/{club_id}", response_model=ClubResponse, summary="Получить клуб")
def get_club(
    club_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.query(Club).filter(Club.id == club_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Клуб не найден")
    
    m_count = db.query(ClubMember).filter(ClubMember.club_id == c.id).count()
    me = db.query(ClubMember).filter(ClubMember.club_id == c.id, ClubMember.user_id == current_user.id).first()
    return ClubResponse(
        id=c.id,
        name=c.name,
        description=c.description,
        avatar_url=c.avatar_url,
        rules=c.rules,
        creator_id=c.creator_id,
        members_count=m_count,
        is_member=me is not None
    )


@router.post("/clubs/{club_id}/join", summary="Вступить в клуб")
def join_club(
    club_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.query(Club).filter(Club.id == club_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Клуб не найден")

    existing = db.query(ClubMember).filter(ClubMember.club_id == club_id, ClubMember.user_id == current_user.id).first()
    if existing:
        return {"status": "ok", "message": "Уже в клубе"}

    m = ClubMember(club_id=club_id, user_id=current_user.id, role="member")
    db.add(m)
    db.commit()
    return {"status": "ok"}


@router.post("/clubs/{club_id}/leave", summary="Покинуть клуб")
def leave_club(
    club_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    m = db.query(ClubMember).filter(ClubMember.club_id == club_id, ClubMember.user_id == current_user.id).first()
    if not m:
        return {"status": "ok", "message": "Не состоит в клубе"}

    club = db.query(Club).filter(Club.id == club_id).first()
    club_name = club.name if club else "Клуб"
    was_sole_admin = False
    if m.role == "admin":
        admin_count = (
            db.query(ClubMember)
            .filter(ClubMember.club_id == club_id, ClubMember.role == "admin")
            .count()
        )
        was_sole_admin = admin_count == 1

    db.delete(m)
    db.flush()

    if was_sole_admin:
        _transfer_admin_if_sole_leaving(db, club_id, club_name)

    db.commit()
    return {"status": "ok"}


@router.get("/clubs/{club_id}/members", response_model=list[ClubMemberResponse], summary="Участники клуба")
def get_club_members(
    club_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.query(Club).filter(Club.id == club_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Клуб не найден")
    
    members = db.query(ClubMember).filter(ClubMember.club_id == club_id).all()
    out = []
    for m in members:
        u = db.query(User).filter(User.id == m.user_id).first()
        if u:
            out.append(ClubMemberResponse(
                id=m.id,
                club_id=m.club_id,
                user=_user_card(db, u, current_user.id),
                role=m.role,
                joined_at=m.joined_at.isoformat() if m.joined_at else ""
            ))
    return out

def _club_admin(db: Session, club_id: int, user_id: int) -> ClubMember:
    member = db.query(ClubMember).filter(ClubMember.club_id == club_id, ClubMember.user_id == user_id).first()
    if not member or member.role != "admin":
        raise HTTPException(status_code=403, detail="Только администратор клуба может выполнить это действие")
    return member


def _club_member(db: Session, club_id: int, user_id: int) -> ClubMember:
    member = db.query(ClubMember).filter(ClubMember.club_id == club_id, ClubMember.user_id == user_id).first()
    if not member:
        raise HTTPException(status_code=403, detail="Вы не состоите в этом клубе")
    return member


def _create_club_notification(
    db: Session,
    *,
    user_id: int,
    club_id: int,
    event_type: str,
    title: str,
    message: str,
    club_name: str,
) -> None:
    db.add(
        ClubNotification(
            user_id=user_id,
            club_id=club_id,
            event_type=event_type,
            title=title,
            message=message,
            club_name=club_name,
        )
    )


def _transfer_admin_if_sole_leaving(db: Session, club_id: int, club_name: str) -> None:
    next_admin = (
        db.query(ClubMember)
        .filter(ClubMember.club_id == club_id)
        .order_by(ClubMember.joined_at.asc())
        .first()
    )
    if not next_admin:
        return
    next_admin.role = "admin"
    _create_club_notification(
        db,
        user_id=next_admin.user_id,
        club_id=club_id,
        event_type="admin_transferred",
        title="Новый администратор",
        message=f"Вы назначены администратором клуба «{club_name}» — прежний админ покинул сообщество",
        club_name=club_name,
    )


@router.put("/clubs/{club_id}", response_model=ClubResponse, summary="Обновить клуб")
def update_club(
    club_id: int,
    payload: ClubUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _club_admin(db, club_id, current_user.id)
    club = db.query(Club).filter(Club.id == club_id).first()
    if not club:
        raise HTTPException(status_code=404, detail="Клуб не найден")
    data = payload.model_dump(exclude_unset=True)
    for key, value in data.items():
        setattr(club, key, value)
    db.commit()
    db.refresh(club)
    m_count = db.query(ClubMember).filter(ClubMember.club_id == club.id).count()
    return ClubResponse(
        id=club.id,
        name=club.name,
        description=club.description,
        avatar_url=club.avatar_url,
        rules=club.rules,
        creator_id=club.creator_id,
        members_count=m_count,
        is_member=True,
    )


@router.patch("/clubs/{club_id}/members/{member_user_id}", summary="Изменить роль участника")
def update_club_member_role(
    club_id: int,
    member_user_id: int,
    payload: ClubMemberRoleUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _club_admin(db, club_id, current_user.id)
    target = db.query(ClubMember).filter(ClubMember.club_id == club_id, ClubMember.user_id == member_user_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="Участник не найден")
    if target.user_id == current_user.id:
        raise HTTPException(status_code=400, detail="Нельзя изменить собственную роль")
    if target.role == "admin" and payload.role == "member":
        admin_count = (
            db.query(ClubMember)
            .filter(ClubMember.club_id == club_id, ClubMember.role == "admin")
            .count()
        )
        if admin_count <= 1:
            raise HTTPException(status_code=400, detail="В клубе должен остаться хотя бы один администратор")

    club = db.query(Club).filter(Club.id == club_id).first()
    club_name = club.name if club else "Клуб"
    old_role = target.role
    target.role = payload.role
    if old_role != payload.role:
        if payload.role == "admin":
            _create_club_notification(
                db,
                user_id=target.user_id,
                club_id=club_id,
                event_type="promoted_admin",
                title="Права администратора",
                message=f"Вам выданы права администратора в клубе «{club_name}»",
                club_name=club_name,
            )
        else:
            _create_club_notification(
                db,
                user_id=target.user_id,
                club_id=club_id,
                event_type="demoted_member",
                title="Изменение роли",
                message=f"В клубе «{club_name}» с вас сняты права администратора",
                club_name=club_name,
            )
    db.commit()
    return {"status": "ok", "role": target.role}


@router.delete("/clubs/{club_id}/members/{member_user_id}", summary="Исключить участника")
def remove_club_member(
    club_id: int,
    member_user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _club_admin(db, club_id, current_user.id)
    target = db.query(ClubMember).filter(ClubMember.club_id == club_id, ClubMember.user_id == member_user_id).first()
    if not target:
        raise HTTPException(status_code=404, detail="Участник не найден")
    if target.user_id == current_user.id:
        raise HTTPException(status_code=400, detail="Нельзя исключить себя — используйте выход из клуба")
    if target.role == "admin":
        admin_count = (
            db.query(ClubMember)
            .filter(ClubMember.club_id == club_id, ClubMember.role == "admin")
            .count()
        )
        if admin_count <= 1:
            raise HTTPException(status_code=400, detail="Сначала назначьте другого администратора")

    club = db.query(Club).filter(Club.id == club_id).first()
    club_name = club.name if club else "Клуб"
    _create_club_notification(
        db,
        user_id=target.user_id,
        club_id=club_id,
        event_type="kicked",
        title="Исключение из клуба",
        message=f"Вас исключили из клуба «{club_name}»",
        club_name=club_name,
    )
    db.delete(target)
    db.commit()
    return {"status": "ok"}


@router.get("/clubs/notifications/recent", response_model=list[ClubNotificationResponse], summary="Недавние уведомления клубов")
def get_club_notifications_recent(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    cutoff = datetime.now(timezone.utc) - timedelta(minutes=30)
    rows = (
        db.query(ClubNotification)
        .filter(
            ClubNotification.user_id == current_user.id,
            ClubNotification.read_at.is_(None),
            ClubNotification.created_at >= cutoff,
        )
        .order_by(ClubNotification.created_at.asc())
        .limit(20)
        .all()
    )
    return [
        ClubNotificationResponse(
            id=n.id,
            club_id=n.club_id,
            event_type=n.event_type,
            title=n.title,
            message=n.message,
            club_name=n.club_name,
            created_at=n.created_at.isoformat() if n.created_at else "",
        )
        for n in rows
    ]


@router.post("/clubs/notifications/{notification_id}/read", summary="Отметить уведомление прочитанным")
def mark_club_notification_read(
    notification_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    n = (
        db.query(ClubNotification)
        .filter(ClubNotification.id == notification_id, ClubNotification.user_id == current_user.id)
        .first()
    )
    if n and n.read_at is None:
        n.read_at = datetime.now(timezone.utc)
        db.commit()
    return {"status": "ok"}


@router.get("/clubs/{club_id}/posts", response_model=list[ClubPostResponse], summary="Посты клуба")
def list_club_posts(
    club_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.query(Club).filter(Club.id == club_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Клуб не найден")
    posts = db.query(ClubPost).filter(ClubPost.club_id == club_id).order_by(ClubPost.created_at.desc()).limit(100).all()
    out = []
    for p in posts:
        u = db.query(User).filter(User.id == p.user_id).first()
        if not u:
            continue
        options = json.loads(p.poll_options) if p.poll_options else None
        votes_raw = json.loads(p.poll_votes) if p.poll_votes else {}
        vote_counts = {k: len(v) if isinstance(v, list) else int(v) for k, v in votes_raw.items()} if votes_raw else None
        my_vote = None
        if isinstance(votes_raw, dict):
            for opt, voters in votes_raw.items():
                if isinstance(voters, list) and current_user.id in voters:
                    my_vote = opt
                    break
        out.append(
            ClubPostResponse(
                id=p.id,
                club_id=p.club_id,
                user=_user_card(db, u, current_user.id),
                post_type=p.post_type,
                body=p.body,
                poll_options=options,
                poll_votes=vote_counts,
                my_vote=my_vote,
                created_at=p.created_at.isoformat() if p.created_at else "",
            )
        )
    return out


@router.post("/clubs/{club_id}/posts", response_model=ClubPostResponse, summary="Создать пост в клубе")
def create_club_post(
    club_id: int,
    payload: ClubPostCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _club_member(db, club_id, current_user.id)
    options_json = None
    votes_json = None
    if payload.post_type == "poll":
        opts = [o.strip() for o in (payload.poll_options or []) if o.strip()]
        if len(opts) < 2:
            raise HTTPException(status_code=400, detail="Опрос должен содержать минимум 2 варианта")
        options_json = json.dumps(opts, ensure_ascii=False)
        votes_json = json.dumps({opt: [] for opt in opts}, ensure_ascii=False)
    post = ClubPost(
        club_id=club_id,
        user_id=current_user.id,
        post_type=payload.post_type,
        body=(payload.body or "").strip() or None,
        poll_options=options_json,
        poll_votes=votes_json,
    )
    db.add(post)
    db.commit()
    db.refresh(post)
    u = db.query(User).filter(User.id == current_user.id).first()
    return ClubPostResponse(
        id=post.id,
        club_id=post.club_id,
        user=_user_card(db, u, current_user.id),
        post_type=post.post_type,
        body=post.body,
        poll_options=json.loads(post.poll_options) if post.poll_options else None,
        poll_votes={k: 0 for k in json.loads(post.poll_votes)} if post.poll_votes else None,
        my_vote=None,
        created_at=post.created_at.isoformat() if post.created_at else "",
    )


@router.post("/clubs/{club_id}/posts/{post_id}/vote", summary="Голос в опросе клуба")
def vote_club_poll(
    club_id: int,
    post_id: int,
    payload: ClubPollVote,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    _club_member(db, club_id, current_user.id)
    post = db.query(ClubPost).filter(ClubPost.id == post_id, ClubPost.club_id == club_id, ClubPost.post_type == "poll").first()
    if not post:
        raise HTTPException(status_code=404, detail="Опрос не найден")
    votes = json.loads(post.poll_votes) if post.poll_votes else {}
    if payload.option not in votes:
        raise HTTPException(status_code=400, detail="Неверный вариант ответа")
    # Один голос на пользователя: снимаем прежний выбор перед записью нового
    for voters in votes.values():
        if isinstance(voters, list) and current_user.id in voters:
            voters.remove(current_user.id)
    votes[payload.option].append(current_user.id)
    post.poll_votes = json.dumps(votes, ensure_ascii=False)
    db.commit()
    return {"status": "ok", "option": payload.option}

@router.post("/challenges/{challenge_id}/leave", summary="Покинуть челлендж")
def leave_challenge(
    challenge_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    p = db.query(ChallengeParticipant).filter(
        ChallengeParticipant.challenge_id == challenge_id, 
        ChallengeParticipant.user_id == current_user.id
    ).first()
    if not p:
        raise HTTPException(status_code=404, detail="Вы не участвуете в этом челлендже")
    
    db.delete(p)
    db.commit()
    return {"status": "ok"}

@router.get("/challenges/{challenge_id}/leaderboard", summary="Таблица лидеров челленджа")
def challenge_leaderboard(
    challenge_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    c = db.query(Challenge).filter(Challenge.id == challenge_id).first()
    if not c:
        raise HTTPException(status_code=404, detail="Челлендж не найден")
        
    participants = db.query(ChallengeParticipant).filter(
        ChallengeParticipant.challenge_id == challenge_id
    ).order_by(ChallengeParticipant.progress.desc()).all()
    
    entries = []
    for idx, p in enumerate(participants):
        user = db.query(User).filter(User.id == p.user_id).first()
        if not user:
            continue
        card = _user_card(db, user, current_user.id)
        entries.append({
            "user_id": p.user_id,
            "display_name": card["display_name"],
            "progress": p.progress,
            "is_me": p.user_id == current_user.id,
            "rank": idx + 1
        })
        
    return {"entries": entries}
