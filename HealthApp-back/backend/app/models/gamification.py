from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, UniqueConstraint
from sqlalchemy.sql import func

from app.db.database import Base


class UserAchievement(Base):
    __tablename__ = "user_achievements"
    __table_args__ = (UniqueConstraint("user_id", "achievement_code", name="uq_user_achievement"),)

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    achievement_code = Column(String(64), nullable=False)
    title = Column(String(128), nullable=False)
    description = Column(String(512), nullable=False)
    icon_key = Column(String(32), nullable=False, default="star")
    points = Column(Integer, nullable=False, default=10)
    unlocked_at = Column(DateTime(timezone=True), server_default=func.now())
    achievement_kind = Column(String(24), nullable=False, default="daily")
    progress_current = Column(Float, nullable=True)
    progress_target = Column(Float, nullable=True)
    progress_unit = Column(String(24), nullable=True)
    record_value = Column(Float, nullable=True)
    record_label = Column(String(128), nullable=True)
