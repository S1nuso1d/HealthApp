from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class UserStateCreate(BaseModel):
    mood: Optional[int] = Field(None, ge=1, le=10, description="Настроение от 1 до 10", examples=[7])
    energy: Optional[int] = Field(None, ge=1, le=10, description="Уровень энергии от 1 до 10", examples=[6])
    stress: Optional[int] = Field(None, ge=1, le=10, description="Уровень стресса от 1 до 10", examples=[4])
    focus: Optional[int] = Field(None, ge=1, le=10, description="Уровень концентрации от 1 до 10", examples=[8])
    record_time: datetime = Field(..., description="Дата и время записи состояния", examples=["2026-04-02T09:00:00"])
    notes: Optional[str] = Field(None, description="Комментарий пользователя", examples=["После хорошего сна чувствую себя бодро"])


class UserStateResponse(BaseModel):
    id: int
    user_id: int
    mood: Optional[int] = None
    energy: Optional[int] = None
    stress: Optional[int] = None
    focus: Optional[int] = None
    record_time: datetime
    notes: Optional[str] = None

    class Config:
        from_attributes = True