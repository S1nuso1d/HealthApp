from datetime import datetime

from pydantic import BaseModel


class MealCreate(BaseModel):
    meal_type: str
    name: str

    calories: float | None = None
    protein_g: float | None = None
    fat_g: float | None = None
    carbs_g: float | None = None
    fiber_g: float | None = None
    sugar_g: float | None = None

    caffeine_mg: float | None = None
    water_ml: float | None = None
    portion_g: float | None = None

    glycemic_load: float | None = None
    meal_category: str | None = None

    minutes_before_sleep: int | None = None
    is_late_meal: bool | None = None

    meal_time: datetime

    notes: str | None = None
    source: str = "manual"


class MealOut(BaseModel):
    id: int
    user_id: int

    meal_type: str
    name: str

    calories: float | None = None
    protein_g: float | None = None
    fat_g: float | None = None
    carbs_g: float | None = None
    fiber_g: float | None = None
    sugar_g: float | None = None

    caffeine_mg: float | None = None
    water_ml: float | None = None
    portion_g: float | None = None

    glycemic_load: float | None = None
    meal_category: str | None = None

    minutes_before_sleep: int | None = None
    is_late_meal: bool | None = None

    meal_time: datetime

    notes: str | None = None
    source: str

    created_at: datetime | None = None

    class Config:
        from_attributes = True