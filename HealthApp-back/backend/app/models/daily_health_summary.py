from sqlalchemy import Column, Date, DateTime, Float, ForeignKey, Index, Integer, Text
from sqlalchemy.sql import func

from app.db.database import Base


class DailyHealthSummary(Base):
    __tablename__ = "daily_health_summaries"
    # Каждый запрос сводки — это «пользователь + диапазон дат».
    # Индекс не уникальный: в существующих базах могли остаться дубли за один день,
    # и падать на старте из-за них приложение не должно.
    __table_args__ = (
        Index("ix_daily_health_summaries_user_date", "user_id", "summary_date"),
    )

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    summary_date = Column(Date, nullable=False, index=True)

    total_sleep_hours = Column(Float, default=0.0)
    average_sleep_score = Column(Float, nullable=True)

    total_water_ml = Column(Integer, default=0)

    total_calories = Column(Float, default=0.0)
    total_caffeine_mg = Column(Float, default=0.0)

    total_steps = Column(Integer, default=0)
    total_active_minutes = Column(Integer, default=0)
    workouts_count = Column(Integer, default=0)

    total_state_score = Column(Float, nullable=True)

    notes = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
    )