from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, model_validator


class SleepCreate(BaseModel):
    sleep_start: datetime = Field(
        ...,
        description="Дата и время засыпания",
        examples=["2026-04-01T23:30:00"]
    )
    sleep_end: datetime = Field(
        ...,
        description="Дата и время пробуждения",
        examples=["2026-04-02T07:30:00"]
    )
    quality_score: Optional[float] = Field(
        None,
        ge=0,
        le=100,
        description="Оценка качества сна от 0 до 100",
        examples=[82]
    )
    deep_sleep_minutes: Optional[int] = Field(
        None,
        ge=0,
        description="Количество минут глубокого сна",
        examples=[110]
    )
    rem_sleep_minutes: Optional[int] = Field(
        None,
        ge=0,
        description="Количество минут быстрого сна",
        examples=[95]
    )
    awakenings_count: Optional[int] = Field(
        None,
        ge=0,
        description="Количество пробуждений за ночь",
        examples=[2]
    )
    sleep_latency_minutes: Optional[int] = Field(
        None,
        ge=0,
        description="Сколько минут заняло засыпание",
        examples=[20]
    )
    awake_time_minutes: Optional[int] = Field(
        None,
        ge=0,
        description="Сколько минут пользователь бодрствовал ночью",
        examples=[15]
    )
    time_in_bed_minutes: Optional[int] = Field(
        None,
        ge=0,
        description="Сколько минут пользователь провел в кровати",
        examples=[500]
    )
    day_type: Optional[str] = Field(
        None,
        description="Тип дня: workday или weekend",
        examples=["workday"]
    )
    notes: Optional[str] = Field(
        None,
        description="Комментарий пользователя",
        examples=["Долго засыпал после поздней тренировки"]
    )
    source: Optional[str] = Field(
        "manual",
        description="Источник данных: manual, health_connect, device",
        examples=["manual"]
    )

    @model_validator(mode="after")
    def validate_dates(self):
        if self.sleep_end <= self.sleep_start:
            raise ValueError("Время пробуждения должно быть позже времени засыпания")
        return self


class SleepResponse(BaseModel):
    id: int
    user_id: int
    sleep_start: datetime
    sleep_end: datetime
    duration_hours: float
    quality_score: Optional[float] = None
    deep_sleep_minutes: Optional[int] = None
    rem_sleep_minutes: Optional[int] = None
    awakenings_count: Optional[int] = None
    sleep_latency_minutes: Optional[int] = None
    awake_time_minutes: Optional[int] = None
    time_in_bed_minutes: Optional[int] = None
    sleep_efficiency: Optional[float] = None
    day_type: Optional[str] = None
    notes: Optional[str] = None
    source: Optional[str] = None

    class Config:
        from_attributes = True