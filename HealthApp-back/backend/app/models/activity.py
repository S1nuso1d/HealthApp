from sqlalchemy import Boolean, Column, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class ActivityRecord(Base):
    __tablename__ = "activity_records"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    activity_type = Column(String, nullable=False, index=True)  # walk, workout, run, gym, cycling, yoga, stretching, other

    start_time = Column(DateTime(timezone=True), nullable=False, index=True)
    end_time = Column(DateTime(timezone=True), nullable=False)

    duration_minutes = Column(Integer, nullable=False)

    steps = Column(Integer, nullable=True)
    distance_km = Column(Float, nullable=True)
    calories_burned = Column(Float, nullable=True)
    avg_heart_rate = Column(Integer, nullable=True)
    avg_power_w = Column(Float, nullable=True)
    avg_speed_m_s = Column(Float, nullable=True)

    intensity = Column(String, nullable=True)  # low, medium, high

    activity_category = Column(String, nullable=True)  # cardio / strength / light / recovery
    perceived_exertion = Column(Integer, nullable=True)  # 1-10

    minutes_before_sleep = Column(Integer, nullable=True)
    is_evening_activity = Column(Boolean, nullable=True, default=False)

    notes = Column(Text, nullable=True)
    source = Column(String, default="manual", nullable=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    user = relationship("User")