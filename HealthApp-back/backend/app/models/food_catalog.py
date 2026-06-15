from datetime import datetime, timezone

from sqlalchemy import Boolean, Column, DateTime, Float, ForeignKey, Integer, String

from app.db.database import Base


class FoodCatalogItem(Base):
    """Пользовательский и дополненный каталог продуктов (по штрихкоду или названию)."""

    __tablename__ = "food_catalog"

    id = Column(Integer, primary_key=True, index=True)
    barcode = Column(String(32), unique=True, nullable=True, index=True)
    name = Column(String(255), nullable=False, index=True)
    brand = Column(String(255), nullable=True)

    calories_100g = Column(Float, nullable=True)
    protein_g_100g = Column(Float, nullable=True)
    fat_g_100g = Column(Float, nullable=True)
    carbs_g_100g = Column(Float, nullable=True)

    image_filename = Column(String(128), nullable=True)
    off_image_url = Column(String(512), nullable=True)

    source = Column(String(32), nullable=False, default="user")
    is_complete = Column(Boolean, nullable=False, default=False)

    created_by_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )
