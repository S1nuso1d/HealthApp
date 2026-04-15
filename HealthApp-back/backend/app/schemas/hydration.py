from datetime import datetime
from pydantic import BaseModel, Field


class HydrationCreate(BaseModel):
    amount_ml: int = Field(
        ...,
        gt=0,
        description="Количество выпитой воды в миллилитрах",
        examples=[200]
    )
    record_time: datetime | None = Field(
        default=None,
        description="Время записи. Если не передано, backend поставит текущее время.",
        examples=["2026-04-06T10:30:00"]
    )
    source: str | None = Field(
        default="manual",
        description="Источник записи: manual / device / import",
        examples=["manual"]
    )


class HydrationResponse(BaseModel):
    id: int = Field(description="ID записи гидратации")
    user_id: int = Field(description="ID пользователя")
    amount_ml: int = Field(description="Количество воды в мл")
    record_time: datetime = Field(description="Время записи")
    source: str | None = Field(default=None, description="Источник записи")

    class Config:
        from_attributes = True


class HydrationSummaryResponse(BaseModel):
    total_ml: int = Field(description="Общее количество воды за сегодня")
    records: list[HydrationResponse] = Field(
        default_factory=list,
        description="Список записей за сегодня"
    )