from sqlalchemy import Boolean, Column, DateTime, Integer, String
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    hashed_password = Column(String, nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    # Refresh-токены, выпущенные раньше этого момента, недействительны.
    # Обновляется при смене пароля, выходе со всех устройств и при попытке
    # повторно использовать уже потраченный refresh-токен.
    tokens_valid_from = Column(DateTime(timezone=True), nullable=True)

    profile = relationship("UserProfile", back_populates="user", uselist=False)
    pill_reminders = relationship("PillReminder", back_populates="user", cascade="all, delete-orphan")