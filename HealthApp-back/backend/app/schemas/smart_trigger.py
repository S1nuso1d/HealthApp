from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel


class SmartTriggerResponse(BaseModel):
    id: int
    user_id: int
    trigger_type: str
    category: str
    title: str
    description: str
    severity: str
    confidence: float
    based_on: Optional[str] = None
    recommended_action: Optional[str] = None
    is_active: bool
    is_resolved: bool
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class SmartReminderResponse(BaseModel):
    id: int
    user_id: int
    trigger_id: Optional[int] = None
    reminder_type: str
    title: str
    message: str
    status: str
    is_active: bool
    remind_at_label: Optional[str] = None
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class SmartTriggerGenerateResponse(BaseModel):
    triggers_created: int
    reminders_created: int


class SmartReminderStatusResponse(BaseModel):
    message: str