from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func

from app.db.database import Base


class ActionPlan(Base):
    __tablename__ = "action_plans"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    category = Column(String, nullable=False, index=True)   # sleep / hydration / meals / activity / state / correlation
    title = Column(String, nullable=False)
    description = Column(Text, nullable=False)

    priority = Column(String, nullable=False, default="medium")   # low / medium / high
    status = Column(String, nullable=False, default="pending")    # pending / in_progress / done / skipped

    action_text = Column(Text, nullable=True)
    source_insight_type = Column(String, nullable=True)
    source_insight_title = Column(String, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    updated_at = Column(
        DateTime(timezone=True),
        server_default=func.now(),
        onupdate=func.now(),
    )