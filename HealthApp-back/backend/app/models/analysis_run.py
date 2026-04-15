from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.db.database import Base


class AnalysisRun(Base):
    __tablename__ = "analysis_runs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    period_days = Column(Integer, nullable=False, default=7)

    summaries_count = Column(Integer, nullable=False, default=0)
    insights_count = Column(Integer, nullable=False, default=0)
    recommendations_count = Column(Integer, nullable=False, default=0)
    smart_triggers_count = Column(Integer, nullable=False, default=0)

    health_score = Column(Float, nullable=True)
    sleep_score = Column(Float, nullable=True)
    hydration_score = Column(Float, nullable=True)
    activity_score = Column(Float, nullable=True)
    nutrition_score = Column(Float, nullable=True)
    state_score = Column(Float, nullable=True)

    status = Column(String, nullable=False, default="completed")
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    user = relationship("User")
    recommendations = relationship(
        "SavedRecommendation",
        back_populates="analysis_run",
        cascade="all, delete-orphan",
    )