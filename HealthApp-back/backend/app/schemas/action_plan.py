from datetime import datetime
from typing import Literal, Optional

from pydantic import BaseModel, Field


ActionCategory = Literal["sleep", "hydration", "meals", "activity", "state", "correlation"]
ActionPriority = Literal["low", "medium", "high"]
ActionStatus = Literal["pending", "in_progress", "done", "skipped"]


class ActionPlanResponse(BaseModel):
    id: int
    user_id: int
    category: ActionCategory
    title: str
    description: str
    priority: ActionPriority
    status: ActionStatus
    action_text: Optional[str] = None
    source_insight_type: Optional[str] = None
    source_insight_title: Optional[str] = None
    created_at: datetime
    updated_at: datetime | None = None

    class Config:
        from_attributes = True


class ActionPlanGenerateResponse(BaseModel):
    message: str
    created_count: int


class ActionPlanStatusUpdate(BaseModel):
    status: ActionStatus = Field(description="Новый статус action plan")