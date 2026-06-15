from sqlalchemy import Column, Integer, String, Boolean, ForeignKey, Time, DateTime, Text, Date
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func
from app.db.database import Base

class PillReminder(Base):
    __tablename__ = "pill_reminders"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False)
    name = Column(String, nullable=False)
    dosage = Column(String, nullable=False)
    time_of_day = Column(Time, nullable=False)
    is_active = Column(Boolean, default=True)

    user = relationship("User", back_populates="pill_reminders")

class CycleEntry(Base):
    """Menstrual cycle tracking entry."""

    __tablename__ = "cycle_entries"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    
    start_date = Column(Date, nullable=False, index=True)
    end_date = Column(Date, nullable=True)
    symptoms = Column(Text, nullable=True) # Could be JSON or comma-separated
    notes = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
    updated_at = Column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
