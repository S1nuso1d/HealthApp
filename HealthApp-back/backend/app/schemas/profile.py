from typing import Optional
from pydantic import BaseModel, Field


class ProfileCreate(BaseModel):
    age: Optional[int] = Field(
        None,
        ge=1,
        le=120,
        description="Возраст пользователя",
        examples=[22]
    )
    sex: Optional[str] = Field(
        None,
        description="Пол пользователя",
        examples=["male"]
    )
    height_cm: Optional[float] = Field(
        None,
        gt=0,
        description="Рост в сантиметрах",
        examples=[180]
    )
    weight_kg: Optional[float] = Field(
        None,
        gt=0,
        description="Вес в килограммах",
        examples=[78]
    )
    goal: Optional[str] = Field(
        None,
        description="Цель пользователя",
        examples=["better_sleep"]
    )
    activity_level: Optional[str] = Field(
        None,
        description="Уровень активности",
        examples=["medium"]
    )
    target_sleep_hours: Optional[float] = Field(
        None,
        gt=0,
        le=24,
        description="Желаемое количество часов сна",
        examples=[8]
    )
    target_water_ml: Optional[float] = Field(
        None,
        gt=0,
        description="Целевая норма воды в мл",
        examples=[2500]
    )
    target_daily_calories: Optional[int] = Field(
        None,
        ge=800,
        le=8000,
        description="Целевая калорийность дня, ккал",
        examples=[2200],
    )
    target_protein_g: Optional[float] = Field(None, ge=0, le=500, description="Цель белка, г/день")
    target_fat_g: Optional[float] = Field(None, ge=0, le=300, description="Цель жиров, г/день")
    target_carbs_g: Optional[float] = Field(None, ge=0, le=800, description="Цель углеводов, г/день")
    target_steps: Optional[int] = Field(
        None,
        ge=1000,
        le=100_000,
        description="Цель шагов в день",
        examples=[10_000],
    )
    is_vegetarian: Optional[bool] = Field(None, description="Вегетарианская диета")
    has_allergies: Optional[bool] = Field(None, description="Есть пищевые аллергии")
    allergies_text: Optional[str] = Field(
        None,
        max_length=500,
        description="На что аллергия / непереносимость",
    )
    onboarding_completed: Optional[bool] = Field(
        None,
        description="Пользователь прошёл стартовый опрос",
    )


class ProfileResponse(BaseModel):
    id: int = Field(description="ID профиля")
    user_id: int = Field(description="ID владельца профиля")
    age: Optional[int] = None
    sex: Optional[str] = None
    height_cm: Optional[float] = None
    weight_kg: Optional[float] = None
    goal: Optional[str] = None
    activity_level: Optional[str] = None
    target_sleep_hours: Optional[float] = None
    target_water_ml: Optional[float] = None
    target_daily_calories: Optional[int] = None
    target_protein_g: Optional[float] = None
    target_fat_g: Optional[float] = None
    target_carbs_g: Optional[float] = None
    target_steps: Optional[int] = None
    is_vegetarian: Optional[bool] = None
    has_allergies: Optional[bool] = None
    allergies_text: Optional[str] = None
    onboarding_completed: bool = False
    has_avatar: bool = Field(False, description="Есть загруженное фото профиля")

    class Config:
        from_attributes = True