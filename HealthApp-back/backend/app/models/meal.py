from sqlalchemy import Boolean, Column, DateTime, Float, ForeignKey, Integer, String, Text
from sqlalchemy.sql import func
from sqlalchemy.orm import relationship

from app.db.database import Base


class MealRecord(Base):
    __tablename__ = "meal_records"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    meal_type = Column(String, nullable=False)  # breakfast, lunch, dinner, snack, drink
    name = Column(String, nullable=False, index=True)

    calories = Column(Float, nullable=True)
    protein_g = Column(Float, nullable=True)
    fat_g = Column(Float, nullable=True)
    carbs_g = Column(Float, nullable=True)
    fiber_g = Column(Float, nullable=True)
    sugar_g = Column(Float, nullable=True)

    caffeine_mg = Column(Float, nullable=True)
    water_ml = Column(Float, nullable=True)
    portion_g = Column(Float, nullable=True)

    glycemic_load = Column(Float, nullable=True)
    meal_category = Column(String, nullable=True)  # healthy / junk / fast_food / homemade

    minutes_before_sleep = Column(Integer, nullable=True)
    is_late_meal = Column(Boolean, nullable=True, default=False)

    meal_time = Column(DateTime(timezone=True), nullable=False, index=True)

    notes = Column(Text, nullable=True)
    source = Column(String, default="manual", nullable=False)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)

    user = relationship("User")