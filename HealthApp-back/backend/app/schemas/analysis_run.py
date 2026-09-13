from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field


class AnalysisRunResponse(BaseModel):
    id: int
    user_id: int
    period_days: int

    summaries_count: int
    insights_count: int
    recommendations_count: int
    smart_triggers_count: int

    health_score: float | None = Field(default=None)
    sleep_score: float | None = Field(default=None)
    hydration_score: float | None = Field(default=None)
    activity_score: float | None = Field(default=None)
    nutrition_score: float | None = Field(default=None)
    state_score: float | None = Field(default=None)

    status: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)