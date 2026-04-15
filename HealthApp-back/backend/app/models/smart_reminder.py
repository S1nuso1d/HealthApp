from sqlalchemy import Boolean, Column, DateTime, ForeignKey, Integer, String
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class SmartReminder(Base):
    __tablename__ = "smart_reminders"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    trigger_id = Column(Integer, ForeignKey("smart_triggers.id"), nullable=True, index=True)

    reminder_type = Column(String, nullable=False)  # hydration / sleep / meal / activity / state / general
    title = Column(String, nullable=False)
    message = Column(String, nullable=False)

    status = Column(String, nullable=False, default="new")  # new / read / completed / dismissed
    is_active = Column(Boolean, default=True)

    remind_at_label = Column(String, nullable=True)  # now / evening / tomorrow / today
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())

    user = relationship("User")
    trigger = relationship("SmartTrigger")