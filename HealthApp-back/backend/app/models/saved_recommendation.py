from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func

from app.db.database import Base


class SavedRecommendation(Base):
    __tablename__ = "saved_recommendations"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    analysis_run_id = Column(Integer, ForeignKey("analysis_runs.id"), nullable=True, index=True)

    category = Column(String, nullable=False, index=True)
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)

    priority = Column(String, nullable=False, default="medium")
    confidence = Column(Float, nullable=True)
    action = Column(Text, nullable=True)

    related_insight_type = Column(String, nullable=True)
    related_insight_title = Column(String, nullable=True)

    status = Column(String, nullable=False, default="new")
    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    user = relationship("User")
    analysis_run = relationship(
        "AnalysisRun",
        back_populates="recommendations",
    )