from datetime import datetime

from pydantic import BaseModel, Field


class SavedDishCreate(BaseModel):
    name: str = Field(..., max_length=255)
    meal_type: str | None = Field(None, max_length=32)
    calories: float | None = None
    protein_g: float | None = None
    fat_g: float | None = None
    carbs_g: float | None = None
    notes: str | None = None


class SavedDishOut(BaseModel):
    id: int
    user_id: int
    name: str
    meal_type: str | None = None
    calories: float | None = None
    protein_g: float | None = None
    fat_g: float | None = None
    carbs_g: float | None = None
    notes: str | None = None
    created_at: datetime | None = None

    class Config:
        from_attributes = True
