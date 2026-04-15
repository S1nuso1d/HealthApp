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

    class Config:
        from_attributes = True