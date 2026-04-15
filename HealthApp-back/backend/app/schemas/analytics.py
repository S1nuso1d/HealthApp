from datetime import date, datetime
from typing import List, Literal, Optional

from pydantic import BaseModel, Field


InsightCategory = Literal["sleep", "meals", "hydration", "activity", "state", "correlation"]
ImpactType = Literal["positive", "negative", "neutral"]
PriorityType = Literal["low", "medium", "high"]
SeverityType = Literal["low", "medium", "high"]


class AnalyticsMeta(BaseModel):
    generated_at: datetime = Field(description="Дата и время генерации аналитики")
    start_date: date = Field(description="Начало периода анализа")
    end_date: date = Field(description="Конец периода анализа")
    data_points: int = Field(description="Количество дней/записей, участвовавших в анализе")
    has_enough_data: bool = Field(description="Достаточно ли данных для уверенного анализа")
    message: Optional[str] = Field(
        default=None,
        description="Дополнительное сообщение, например если данных недостаточно"
    )


class AnalyticsEvidence(BaseModel):
    metric: str = Field(description="Название метрики")
    value: float = Field(description="Текущее или вычисленное значение")
    unit: Optional[str] = Field(default=None, description="Единица измерения")
    note: Optional[str] = Field(default=None, description="Пояснение по метрике")


class InsightItem(BaseModel):
    category: InsightCategory = Field(
        description="Категория инсайта: sleep, meals, hydration, activity, state, correlation"
    )
    title: str = Field(description="Краткий заголовок инсайта")
    description: str = Field(description="Описание найденной закономерности")
    confidence: float = Field(ge=0.0, le=1.0, description="Уровень уверенности от 0 до 1")
    impact: ImpactType = Field(description="Влияние: positive, negative, neutral")
    severity: SeverityType = Field(description="Серьезность: low, medium, high")
    evidence: List[AnalyticsEvidence] = Field(
        default_factory=list,
        description="Доказательства и опорные метрики для инсайта"
    )


class RecommendationItem(BaseModel):
    category: InsightCategory = Field(description="Категория рекомендации")
    title: str = Field(description="Краткий заголовок рекомендации")
    description: str = Field(description="Подробное описание рекомендации")
    priority: PriorityType = Field(description="Приоритет: low, medium, high")
    confidence: float = Field(ge=0.0, le=1.0, description="Уровень уверенности от 0 до 1")
    action: Optional[str] = Field(
        default=None,
        description="Конкретное действие для пользователя"
    )
    related_insight_title: Optional[str] = Field(
        default=None,
        description="Связанный инсайт, на основе которого сформирована рекомендация"
    )
    related_insight_type: Optional[str] = Field(
        default=None,
        description="Тип связанного инсайта"
    )


class AnalyticsSummary(BaseModel):
    period_days: int = Field(ge=1, description="Период анализа в днях")
    health_score: int = Field(ge=0, le=100, description="Общий health score от 0 до 100")
    sleep_score: int = Field(ge=0, le=100, description="Оценка сна от 0 до 100")
    hydration_score: int = Field(ge=0, le=100, description="Оценка гидратации от 0 до 100")
    activity_score: int = Field(ge=0, le=100, description="Оценка активности от 0 до 100")
    nutrition_score: int = Field(ge=0, le=100, description="Оценка питания от 0 до 100")
    state_score: int = Field(ge=0, le=100, description="Оценка субъективного состояния от 0 до 100")


class AnalyticsResponse(BaseModel):
    meta: AnalyticsMeta
    summary: AnalyticsSummary
    insights: List[InsightItem]
    recommendations: List[RecommendationItem]