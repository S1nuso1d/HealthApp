from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class SleepRecord(Base):
    __tablename__ = "sleep_records"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    sleep_start = Column(DateTime(timezone=True), nullable=False)
    sleep_end = Column(DateTime(timezone=True), nullable=False)

    duration_hours = Column(Float, nullable=False)

    quality_score = Column(Float, nullable=True)
    deep_sleep_minutes = Column(Integer, nullable=True)
    rem_sleep_minutes = Column(Integer, nullable=True)
    awakenings_count = Column(Integer, nullable=True)

    sleep_latency_minutes = Column(Integer, nullable=True)
    awake_time_minutes = Column(Integer, nullable=True)
    time_in_bed_minutes = Column(Integer, nullable=True)
    sleep_efficiency = Column(Float, nullable=True)

    day_type = Column(String, nullable=True)  # workday / weekend

    notes = Column(String, nullable=True)
    source = Column(String, default="manual")

    created_at = Column(DateTime(timezone=True), server_default=func.now())

    user = relationship("User")