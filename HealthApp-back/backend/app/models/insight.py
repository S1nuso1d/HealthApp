from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func

from app.db.database import Base


class Insight(Base):
    __tablename__ = "insights"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    insight_type = Column(String, nullable=False, index=True)
    category = Column(String, nullable=False, default="correlation", index=True)

    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)

    confidence = Column(Float, default=0.0)
    severity = Column(String, default="low")  # low / medium / high
    impact = Column(String, default="neutral")  # positive / negative / neutral

    evidence_json = Column(Text, nullable=True)

    window_days = Column(Integer, default=7)

    created_at = Column(DateTime(timezone=True), server_default=func.now())