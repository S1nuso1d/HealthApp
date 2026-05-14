from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


class AIChatRequest(BaseModel):
    question: str = Field(description="Вопрос пользователя к AI-ассистенту")
    period_days: int = Field(default=7, ge=1, le=60, description="Период анализа в днях")


class AIExplainInsightRequest(BaseModel):
    insight_title: str = Field(description="Заголовок инсайта, который нужно объяснить")
    period_days: int = Field(default=7, ge=1, le=60, description="Период анализа в днях")


class AIResponse(BaseModel):
    answer: str = Field(description="Ответ AI")
    generated_at: datetime = Field(description="Время генерации ответа")
    source: str = Field(default="llm", description="Источник ответа")


class AIBriefResponse(BaseModel):
    title: str
    summary: str
    key_points: List[str]
    generated_at: datetime
    source: str = "llm"


class AIRecommendationItem(BaseModel):
    category: str
    title: str
    description: str
    priority: str
    status: str = "active"
    confidence: Optional[float] = None
    action: Optional[str] = None
    personalized_tip: Optional[str] = None
    progress_label: Optional[str] = None
    related_insight_title: Optional[str] = None
    related_insight_type: Optional[str] = None


class AIRecommendationsResponse(BaseModel):
    generated_at: datetime
    period_days: int
    health_score: int
    recommendations: List[AIRecommendationItem]


class LLMContextSnapshot(BaseModel):
    period_days: int
    health_score: int
    sleep_score: int
    hydration_score: int
    activity_score: int
    nutrition_score: int
    state_score: int
    insights_count: int
    recommendations_count: int