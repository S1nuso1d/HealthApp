from sqlalchemy import Boolean, Column, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func

from app.db.database import Base


class Friendship(Base):
    __tablename__ = "friendships"

    id = Column(Integer, primary_key=True, index=True)
    requester_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    addressee_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    status = Column(String(20), nullable=False, default="pending")  # pending | accepted | blocked
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())


class UserPrivacySettings(Base):
    __tablename__ = "user_privacy_settings"

    user_id = Column(Integer, ForeignKey("users.id"), primary_key=True)
    profile_visibility = Column(String(20), nullable=False, default="friends")  # public | friends | private
    feed_visibility = Column(String(20), nullable=False, default="friends")
    show_activity_to_friends = Column(Boolean, nullable=False, default=True)
    show_achievements_to_friends = Column(Boolean, nullable=False, default=True)


class FeedPost(Base):
    __tablename__ = "feed_posts"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    body = Column(Text, nullable=True)
    media_url = Column(String(512), nullable=True)
    media_type = Column(String(20), nullable=True)  # image | video
    activity_id = Column(Integer, ForeignKey("activity_records.id"), nullable=True)
    visibility = Column(String(20), nullable=False, default="friends")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
