from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field


class ScoreDeltaItem(BaseModel):
    metric: str = Field(description="Название метрики")
    previous_value: int = Field(description="Предыдущее значение")
    current_value: int = Field(description="Текущее значение")
    delta: int = Field(description="Разница между текущим и предыдущим значением")
    trend: str = Field(description="Направление изменения: improved, declined, unchanged")


class ProgressInsightItem(BaseModel):
    metric: str = Field(description="Метрика или категория")
    title: str = Field(description="Краткий заголовок изменения")
    description: str = Field(description="Описание прогресса или ухудшения")
    impact: str = Field(description="Тип влияния: positive, negative, neutral")


class RecommendationStatusItem(BaseModel):
    title: str = Field(description="Название рекомендации")
    category: str = Field(description="Категория рекомендации")
    previous_exists: bool = Field(description="Была ли рекомендация в предыдущем анализе")
    current_exists: bool = Field(description="Есть ли рекомендация в текущем анализе")
    status: str = Field(description="Статус: resolved, new, persistent")


class AnalysisCompareSummary(BaseModel):
    previous_run_id: int = Field(description="ID предыдущего анализа")
    current_run_id: int = Field(description="ID текущего анализа")
    previous_created_at: datetime = Field(description="Дата предыдущего анализа")
    current_created_at: datetime = Field(description="Дата текущего анализа")
    overall_trend: str = Field(description="Общий тренд: improved, declined, mixed, unchanged")
    health_score_delta: int = Field(description="Изменение общего health score")


class AnalysisCompareResponse(BaseModel):
    summary: AnalysisCompareSummary
    score_deltas: List[ScoreDeltaItem]
    progress_insights: List[ProgressInsightItem]
    recommendation_statuses: List[RecommendationStatusItem]