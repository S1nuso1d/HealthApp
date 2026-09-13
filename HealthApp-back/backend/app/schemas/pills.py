from pydantic import BaseModel, ConfigDict
from datetime import time

class PillReminderBase(BaseModel):
    name: str
    dosage: str
    time_of_day: time
    is_active: bool = True

class PillReminderCreate(PillReminderBase):
    pass

class PillReminderUpdate(BaseModel):
    name: str | None = None
    dosage: str | None = None
    time_of_day: time | None = None
    is_active: bool | None = None

class PillReminderOut(PillReminderBase):
    id: int
    user_id: int

    model_config = ConfigDict(from_attributes=True)
