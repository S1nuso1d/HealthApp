from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel, Field
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.activity import ActivityRecord
from app.models.gamification import UserAchievement
from app.models.profile import UserProfile
from app.models.social import FeedPost, Friendship, UserPrivacySettings
from app.models.user import User
router = APIRouter(prefix="/social", tags=["Social"])


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


def _can_view_profile(db: Session, viewer_id: int, target_id: int) -> bool:
    if viewer_id == target_id:
        return True
    p = _privacy(db, target_id)
    if p.profile_visibility == "public":
        return True
    if p.profile_visibility == "private":
        return False
    return _are_friends(db, viewer_id, target_id)


def _can_view_feed(db: Session, viewer_id: int, author_id: int, post_visibility: str) -> bool:
    if viewer_id == author_id:
        return True
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
    return _are_friends(db, viewer_id, author_id)


def _user_card(db: Session, user: User, viewer_id: int) -> dict:
    profile = db.query(UserProfile).filter(UserProfile.user_id == user.id).first()
    email = user.email or ""
    masked = email.split("@")[0][:3] + "***@" + email.split("@")[-1] if "@" in email else f"user{user.id}"
    return {
        "user_id": user.id,
        "display_name": masked,
        "goal": profile.goal if profile else None,
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
    pattern = f"%{q.strip().lower()}%"
    users = (
        db.query(User)
        .filter(User.id != current_user.id, User.email.ilike(pattern))
        .limit(20)
        .all()
    )
    return {"users": [_user_card(db, u, current_user.id) for u in users]}


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


@router.get("/feed")
def get_feed(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    posts = db.query(FeedPost).order_by(FeedPost.created_at.desc()).limit(80).all()
    items = []
    for p in posts:
        if not _can_view_feed(db, current_user.id, p.user_id, p.visibility):
            continue
        author = db.query(User).filter(User.id == p.user_id).first()
        if not author:
            continue
        act = None
        if p.activity_id:
            a = db.query(ActivityRecord).filter(ActivityRecord.id == p.activity_id).first()
            if a:
                act = {
                    "activity_type": a.activity_type,
                    "duration_minutes": a.duration_minutes,
                    "calories_burned": float(a.calories_burned or 0),
                    "steps": a.steps,
                }
        items.append(
            {
                "id": p.id,
                "author": _user_card(db, author, current_user.id),
                "body": p.body,
                "media_url": p.media_url,
                "media_type": p.media_type,
                "activity": act,
                "created_at": p.created_at.isoformat() if p.created_at else None,
            }
        )
    return {"posts": items[:40]}


@router.post("/feed")
def create_post(
    body: FeedPostCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
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


@router.get("/users/{user_id}/profile")
def friend_profile(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(404, detail="Пользователь не найден")
    if not _can_view_profile(db, current_user.id, user_id):
        raise HTTPException(403, detail="Профиль закрыт настройками приватности")
    privacy = _privacy(db, user_id)
    card = _user_card(db, user, current_user.id)
    activities = []
    achievements = []
    if privacy.show_activity_to_friends or current_user.id == user_id:
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
    if privacy.show_achievements_to_friends or current_user.id == user_id:
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
    return {
        "user": card,
        "activities": activities,
        "achievements": achievements,
        "is_friend": _are_friends(db, current_user.id, user_id),
    }
