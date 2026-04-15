from sqlalchemy import Column, DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class UserState(Base):
    __tablename__ = "user_states"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    mood = Column(Integer, nullable=True)       # 1-10
    energy = Column(Integer, nullable=True)     # 1-10
    stress = Column(Integer, nullable=True)     # 1-10
    focus = Column(Integer, nullable=True)      # 1-10
    wellbeing = Column(Integer, nullable=True)  # 1-10

    record_time = Column(DateTime(timezone=True), nullable=False, index=True)

    notes = Column(Text, nullable=True)
    source = Column(String, default="manual", nullable=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    user = relationship("User")