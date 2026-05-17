from datetime import datetime

from sqlalchemy import Column, DateTime, Integer, String

from app.db.database import Base


class PendingRegistration(Base):
    """Временная запись до подтверждения email (код из письма)."""

    __tablename__ = "pending_registrations"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    code = Column(String(6), nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(DateTime(timezone=True), nullable=False)
