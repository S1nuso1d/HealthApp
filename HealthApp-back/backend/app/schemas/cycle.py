from pydantic import BaseModel, ConfigDict
from datetime import date, datetime
from typing import Optional

class CycleEntryBase(BaseModel):
    start_date: date
    end_date: Optional[date] = None
    symptoms: Optional[str] = None
    notes: Optional[str] = None

class CycleEntryCreate(CycleEntryBase):
    pass

class CycleEntryUpdate(BaseModel):
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    symptoms: Optional[str] = None
    notes: Optional[str] = None

class CycleEntryResponse(CycleEntryBase):
    id: int
    user_id: int
    created_at: datetime
    updated_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)
