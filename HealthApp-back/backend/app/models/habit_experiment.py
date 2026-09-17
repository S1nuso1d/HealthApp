from sqlalchemy import Column, Date, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func

from app.db.database import Base


class HabitExperiment(Base):
    """Недельная попытка убрать один фактор и сравнить сон или энергию."""

    __tablename__ = "habit_experiments"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    factor_id = Column(String, nullable=False, index=True)
    title = Column(String, nullable=False)
    action = Column(Text, nullable=False)
    metric = Column(String, nullable=False, default="sleep_hours")

    status = Column(String, nullable=False, default="active", index=True)

    started_on = Column(Date, nullable=False)
    ends_on = Column(Date, nullable=False)

    baseline_value = Column(Float, nullable=True)
    result_value = Column(Float, nullable=True)
    result_summary = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    updated_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
    )
