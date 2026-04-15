from sqlalchemy import Boolean, Column, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class HydrationRecord(Base):
    __tablename__ = "hydration_records"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    amount_ml = Column(Float, nullable=False)
    drink_type = Column(String, nullable=False, default="water", index=True)
    hydration_factor = Column(Float, nullable=True, default=1.0)

    is_late_drink = Column(Boolean, nullable=True, default=False)

    record_time = Column(DateTime(timezone=True), nullable=False, index=True)

    notes = Column(Text, nullable=True)
    source = Column(String, default="manual", nullable=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    user = relationship("User")