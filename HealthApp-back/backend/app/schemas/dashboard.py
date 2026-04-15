from typing import List, Optional

from pydantic import BaseModel

from app.schemas.action_plan import ActionPlanResponse
from app.schemas.ai import AIBriefResponse
from app.schemas.analytics import AnalyticsResponse


class SmartTriggerItem(BaseModel):
    type: str
    title: str
    description: str
    severity: str
    confidence: float | None = None


class SmartReminderItem(BaseModel):
    type: str
    title: str
    message: str
    recommended_time: str | None = None


class DashboardHomeResponse(BaseModel):
    analytics: AnalyticsResponse
    action_plan: List[ActionPlanResponse]
    daily_brief: Optional[AIBriefResponse] = None
    smart_triggers: List[SmartTriggerItem]
    smart_reminders: List[SmartReminderItem]