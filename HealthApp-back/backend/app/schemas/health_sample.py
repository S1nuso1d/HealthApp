from datetime import datetime

from pydantic import BaseModel, Field


class HealthSampleCreate(BaseModel):
    """Одна выборка метрики (источник обычно health_connect).

    Коды metric: heart_rate_bpm, blood_pressure_mmhg (value1 сист., value2 диаст.),
    spo2_percent, blood_glucose_mmol_l, weight_kg, height_cm, body_fat_percent,
    bmr_kcal, vo2_max, power_w, speed_m_s, distance_m, active_calories_kcal,
    total_calories_kcal.
    """

    recorded_at: datetime
    period_end: datetime | None = None
    metric: str = Field(..., max_length=64)
    value1: float | None = None
    value2: float | None = None
    text_value: str | None = None
    source: str = "health_connect"


class HealthSampleOut(BaseModel):
    id: int
    user_id: int
    recorded_at: datetime
    period_end: datetime | None = None
    metric: str
    value1: float | None = None
    value2: float | None = None
    text_value: str | None = None
    source: str
    created_at: datetime | None = None

    class Config:
        from_attributes = True
