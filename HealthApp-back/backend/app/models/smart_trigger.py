from sqlalchemy import Boolean, Column, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class SmartTrigger(Base):
    __tablename__ = "smart_triggers"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    trigger_type = Column(String, nullable=False)
    category = Column(String, nullable=False)
    title = Column(String, nullable=False)
    description = Column(String, nullable=False)

    severity = Column(String, nullable=False, default="medium")  # low / medium / high
    confidence = Column(Float, nullable=False, default=0.7)

    based_on = Column(String, nullable=True)
    recommended_action = Column(String, nullable=True)

    is_active = Column(Boolean, default=True)
    is_resolved = Column(Boolean, default=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    user = relationship("User")