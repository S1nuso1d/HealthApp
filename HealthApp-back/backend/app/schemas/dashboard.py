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


class GoalsCalendarMealItem(BaseModel):
    name: str
    meal_type: str
    meal_time: str
    calories: float = 0.0
    protein_g: float = 0.0
    fat_g: float = 0.0
    carbs_g: float = 0.0


class GoalsCalendarDay(BaseModel):
    date: str
    sleep_met: bool = False
    hydration_met: bool = False
    activity_met: bool = False
    nutrition_met: bool = False
    all_goals_met: bool = False
    has_any_data: bool = False
    sleep_progress: float = 0.0
    hydration_progress: float = 0.0
    activity_progress: float = 0.0
    nutrition_progress: float = 0.0
    sleep_hours: float = 0.0
    water_ml: int = 0
    steps: int = 0
    calories: float = 0.0
    calories_burned: float = 0.0
    calories_consumed: float = 0.0
    protein_g: float = 0.0
    fat_g: float = 0.0
    carbs_g: float = 0.0
    meals: List[GoalsCalendarMealItem] = []


class GoalsCalendarResponse(BaseModel):
    days: List[GoalsCalendarDay]