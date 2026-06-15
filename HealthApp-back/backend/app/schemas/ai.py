from datetime import datetime
from typing import List, Literal, Optional

from pydantic import BaseModel, Field


class ChatHistoryMessage(BaseModel):
    role: Literal["user", "assistant"] = Field(description="Роль в диалоге")
    content: str = Field(min_length=1, max_length=4000, description="Текст сообщения")


class AIChatRequest(BaseModel):
    question: str = Field(min_length=1, max_length=4000, description="Вопрос пользователя к AI-ассистенту")
    period_days: int = Field(default=14, ge=1, le=60, description="Период анализа в днях")
    history: List[ChatHistoryMessage] = Field(
        default_factory=list,
        max_length=24,
        description="Предыдущие реплики диалога (без текущего вопроса)",
    )


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


class MealPlanItem(BaseModel):
    meal_type: str = Field(description="Тип приема пищи (Завтрак, Обед, Перекус, Ужин)")
    name: str = Field(description="Название блюда")
    calories: int = Field(description="Ккал")
    protein_g: float = Field(description="Белки (г)")
    fat_g: float = Field(description="Жиры (г)")
    carbs_g: float = Field(description="Углеводы (г)")
    recipe: Optional[str] = Field(None, description="Краткий рецепт")

class MealPlanDay(BaseModel):
    day_name: str = Field(description="Название дня (Понедельник, Вторник...)")
    meals: List[MealPlanItem]
    total_calories: int
    total_protein: float
    total_fat: float
    total_carbs: float

class GroceryListItem(BaseModel):
    category: str = Field(description="Овощи, Мясо, Молочка и т.д.")
    name: str = Field(description="Название продукта")
    amount: str = Field(description="Примерное количество (шт, г, мл)")

class MealPlanResponse(BaseModel):
    generated_at: datetime
    days: List[MealPlanDay]
    grocery_list: List[GroceryListItem]
    source: str = Field(default="llm", description="llm или fallback")

class WorkoutPlanItem(BaseModel):
    day_name: str
    workout_type: str = Field(description="Кардио, Силовая, Йога, Отдых")
    title: str
    duration_minutes: int
    description: str

class WorkoutPlanResponse(BaseModel):
    generated_at: datetime
    workouts: List[WorkoutPlanItem]


class AiRecognizedFoodItem(BaseModel):
    name: str = Field(description="Название продукта/блюда")
    grams: int = Field(description="Примерный вес в граммах")
    calories: int = Field(description="Ккал")
    protein: float = Field(description="Белки (г)")
    fat: float = Field(description="Жиры (г)")
    carbs: float = Field(description="Углеводы (г)")

class AiRecognizedFoodResponse(BaseModel):
    items: List[AiRecognizedFoodItem]

class AiRecognizeTextFoodRequest(BaseModel):
    text: str = Field(description="Текст с описанием съеденного")

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

class DashboardHintsResponse(BaseModel):
    hints: List[str] = Field(description="Список контекстных подсказок")

class ProactiveTipResponse(BaseModel):
    tip: str = Field(description="Проактивный совет от AI")
    generated_at: datetime = Field(description="Время генерации")


class AiStatusResponse(BaseModel):
    llm_enabled: bool
    llm_provider: str
    llm_model: str
    llm_available: bool
    fallback_enabled: bool
    message: str = Field(description="Статус для UI")

class SleepSoundRecord(BaseModel):
    time: str = Field(description="Время (например, '02:30')")
    label: str = Field(description="Тип звука (например, 'Храп', 'Разговор')")
    peakRms: Optional[float] = Field(default=None, description="Максимальная громкость")

class SleepSummaryRequest(BaseModel):
    sounds: List[SleepSoundRecord] = Field(description="Список зафиксированных звуков")

class SleepSummaryResponse(BaseModel):
    summary: str = Field(description="Сгенерированное саммари")
    generated_at: datetime = Field(description="Время генерации")
