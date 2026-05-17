from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func

from app.db.database import Base


class HealthSample(Base):
    """Универсальные выборки с часов/Health Connect (пульс, SpO₂, калории за интервал и т.д.)."""

    __tablename__ = "health_samples"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    recorded_at = Column(DateTime(timezone=True), nullable=False, index=True)
    period_end = Column(DateTime(timezone=True), nullable=True)
    metric = Column(String(64), nullable=False, index=True)
    value1 = Column(Float, nullable=True)
    value2 = Column(Float, nullable=True)
    text_value = Column(Text, nullable=True)
    source = Column(String(64), nullable=False, default="health_connect")

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
