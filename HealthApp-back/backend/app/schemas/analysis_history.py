from datetime import datetime
from typing import Optional, List
from pydantic import BaseModel, Field


class SavedRecommendationResponse(BaseModel):
    id: int
    analysis_run_id: int
    user_id: int
    item_type: str
    category: str
    title: str
    description: str
    priority: Optional[str] = None
    impact: Optional[str] = None
    confidence: float
    action: Optional[str] = None

    why_this: Optional[str] = None
    based_on: Optional[str] = None
    expected_effect: Optional[str] = None

    status: str
    is_read: bool
    is_active: bool
    created_at: datetime
    updated_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class AnalysisRunResponse(BaseModel):
    id: int
    user_id: int
    period_days: int
    health_score: int
    sleep_score: int
    hydration_score: int
    activity_score: int
    nutrition_score: int
    state_score: int
    created_at: datetime

    class Config:
        from_attributes = True


class AnalysisRunWithItemsResponse(BaseModel):
    run: AnalysisRunResponse
    items: List[SavedRecommendationResponse]


class MarkRecommendationReadResponse(BaseModel):
    message: str = Field(description="Результат обновления статуса")