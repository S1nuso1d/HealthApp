from datetime import datetime

from pydantic import BaseModel


class ActivityCreate(BaseModel):
    activity_type: str
    start_time: datetime
    end_time: datetime

    duration_minutes: int

    steps: int | None = None
    distance_km: float | None = None
    calories_burned: float | None = None
    avg_heart_rate: int | None = None
    avg_power_w: float | None = None
    avg_speed_m_s: float | None = None

    intensity: str | None = None
    activity_category: str | None = None
    perceived_exertion: int | None = None

    minutes_before_sleep: int | None = None
    is_evening_activity: bool | None = None

    notes: str | None = None
    source: str = "manual"


class ActivityOut(BaseModel):
    id: int
    user_id: int

    activity_type: str
    start_time: datetime
    end_time: datetime

    duration_minutes: int

    steps: int | None = None
    distance_km: float | None = None
    calories_burned: float | None = None
    avg_heart_rate: int | None = None
    avg_power_w: float | None = None
    avg_speed_m_s: float | None = None

    intensity: str | None = None
    activity_category: str | None = None
    perceived_exertion: int | None = None

    minutes_before_sleep: int | None = None
    is_evening_activity: bool | None = None

    notes: str | None = None
    source: str

    created_at: datetime | None = None

    class Config:
        from_attributes = True