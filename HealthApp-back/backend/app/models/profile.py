from sqlalchemy import Boolean, Column, Float, ForeignKey, Integer, String
from sqlalchemy.orm import relationship

from app.db.database import Base


class UserProfile(Base):
    __tablename__ = "user_profiles"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), unique=True, nullable=False)

    first_name = Column(String(64), nullable=True)
    last_name = Column(String(64), nullable=True)
    nickname = Column(String(32), nullable=True, index=True)

    age = Column(Integer, nullable=True)
    birth_date = Column(String(10), nullable=True)
    sex = Column(String, nullable=True)
    height_cm = Column(Float, nullable=True)
    weight_kg = Column(Float, nullable=True)
    goal = Column(String, nullable=True)
    activity_level = Column(String, nullable=True)
    target_sleep_hours = Column(Float, nullable=True)
    target_water_ml = Column(Float, nullable=True)

    target_daily_calories = Column(Integer, nullable=True)
    target_protein_g = Column(Float, nullable=True)
    target_fat_g = Column(Float, nullable=True)
    target_carbs_g = Column(Float, nullable=True)
    target_steps = Column(Integer, nullable=True)

    is_vegetarian = Column(Boolean, nullable=True)
    has_allergies = Column(Boolean, nullable=True)
    allergies_text = Column(String, nullable=True)
    onboarding_completed = Column(Boolean, nullable=False, default=False)

    current_streak = Column(Integer, nullable=False, default=0)
    last_active_date = Column(String(10), nullable=True)

    has_avatar = Column(Boolean, nullable=False, default=False)

    user = relationship("User", back_populates="profile")