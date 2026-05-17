from sqlalchemy import Column, DateTime, Float, ForeignKey, Integer, String, Text, func

from app.db.database import Base


class SavedDish(Base):
    """Сохранённые блюда / шаблоны порций пользователя."""

    __tablename__ = "saved_dishes"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)

    name = Column(String(255), nullable=False)
    meal_type = Column(String(32), nullable=True)
    calories = Column(Float, nullable=True)
    protein_g = Column(Float, nullable=True)
    fat_g = Column(Float, nullable=True)
    carbs_g = Column(Float, nullable=True)
    notes = Column(Text, nullable=True)

    created_at = Column(DateTime(timezone=True), server_default=func.now(), index=True)
