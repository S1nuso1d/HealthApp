import functools
import json
from datetime import date, datetime, timedelta, timezone

import anyio.to_thread
from fastapi import APIRouter, Depends, HTTPException, Query, status, UploadFile, File
from fastapi.responses import StreamingResponse
import base64
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.rate_limit import ai_rate_limit
from app.services.avatar_storage import validate_avatar_bytes
from app.db.database import get_db
from app.llm.health_chat_service import HealthChatService
from app.llm.llm_client import LLMClientError
from app.models.daily_health_summary import DailyHealthSummary
from app.models.insight import Insight
from app.models.user import User
from app.models.user_state import UserState
from app.services.analytics.user_trends_service import compute_user_trends
from app.services.recommendation_orchestrator import (
    build_dynamic_ai_recommendations,
    build_merged_recommendation_items,
)
from app.schemas.ai import (
    AIBriefResponse,
    AIChatRequest,
    AIExplainInsightRequest,
    AIRecommendationsResponse,
    AIResponse,
    AiRecognizedFoodResponse,
    AiRecognizeTextFoodRequest,
    AiStatusResponse,
    MealPlanResponse,
    WorkoutPlanResponse,
    DashboardHintsResponse,
    ProactiveTipResponse,
    SleepSummaryRequest,
    SleepSummaryResponse,
)
from app.schemas.analytics import (
    AnalyticsEvidence,
    AnalyticsMeta,
    AnalyticsResponse,
    AnalyticsSummary,
    InsightItem,
)

router = APIRouter(prefix="/ai", tags=["AI"])


def clamp_score(value: float) -> int:
    return max(0, min(100, round(value)))


def _shorten_dashboard_hint(text: str, max_len: int = 72) -> str:
    compact = " ".join(str(text).split())
    if len(compact) <= max_len:
        return compact
    trimmed = compact[:max_len].rsplit(" ", 1)[0]
    return trimmed.rstrip(".,!? ") + "…"


def calculate_sleep_score(avg_sleep_hours: float) -> int:
    if avg_sleep_hours <= 0:
        return 0
    if avg_sleep_hours >= 8:
        return 100
    return clamp_score((avg_sleep_hours / 8.0) * 100)


def calculate_hydration_score(avg_water_ml: float) -> int:
    if avg_water_ml <= 0:
        return 0
    if avg_water_ml >= 2500:
        return 100
    return clamp_score((avg_water_ml / 2500.0) * 100)


def calculate_activity_score(avg_steps: float) -> int:
    if avg_steps <= 0:
        return 0
    if avg_steps >= 10000:
        return 100
    return clamp_score((avg_steps / 10000.0) * 100)


def calculate_nutrition_score(avg_caffeine_mg: float) -> int:
    if avg_caffeine_mg <= 100:
        return 90
    if avg_caffeine_mg <= 200:
        return 75
    if avg_caffeine_mg <= 300:
        return 60
    if avg_caffeine_mg <= 400:
        return 45
    return 30


def calculate_state_score(db: Session, user_id: int, start_date: date, end_date: date) -> int:
    states = (
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= datetime.combine(start_date, datetime.min.time()),
            UserState.record_time <= datetime.combine(end_date, datetime.max.time()),
        )
        .all()
    )

    values = []
    for state in states:
        for attr in ("energy", "mood", "stress", "focus", "wellbeing"):
            value = getattr(state, attr, None)
            if value is not None:
                if attr == "stress":
                    values.append(11.0 - float(value))
                else:
                    values.append(float(value))

    if not values:
        return 50

    avg_state = sum(values) / len(values)
    return clamp_score((avg_state / 10.0) * 100)


def parse_evidence(item: Insight) -> list[AnalyticsEvidence]:
    if not item.evidence_json:
        return []

    try:
        raw = json.loads(item.evidence_json)
        return [AnalyticsEvidence(**entry) for entry in raw]
    except Exception:
        return []


def build_insight_items(db_insights: list[Insight]) -> list[InsightItem]:
    return [
        InsightItem(
            category=item.category,
            title=item.title,
            description=item.description,
            confidence=item.confidence,
            impact=item.impact,
            severity=item.severity,
            evidence=parse_evidence(item),
        )
        for item in db_insights
    ]


def build_analytics_context(
    db: Session,
    user_id: int,
    days: int,
) -> AnalyticsResponse:
    end_date = date.today()
    start_date = end_date - timedelta(days=days - 1)

    summaries = (
        db.query(DailyHealthSummary)
        .filter(
            DailyHealthSummary.user_id == user_id,
            DailyHealthSummary.summary_date >= start_date,
            DailyHealthSummary.summary_date <= end_date,
        )
        .order_by(DailyHealthSummary.summary_date.asc())
        .all()
    )

    insights = (
        db.query(Insight)
        .filter(Insight.user_id == user_id)
        .order_by(Insight.created_at.desc())
        .all()
    )

    from app.services.health_score_service import compute_today_scores

    today_scores = compute_today_scores(db=db, user_id=user_id)
    sleep_score = today_scores["sleep_score"]
    hydration_score = today_scores["hydration_score"]
    activity_score = today_scores["activity_score"]
    nutrition_score = today_scores["nutrition_score"]
    state_score = today_scores["state_score"]
    health_score = today_scores["health_score"]

    recommendations = build_merged_recommendation_items(db=db, user_id=user_id, days=days)
    trends = compute_user_trends(db=db, user_id=user_id, days=days)

    return AnalyticsResponse(
        meta=AnalyticsMeta(
            generated_at=datetime.now(timezone.utc),
            start_date=start_date,
            end_date=end_date,
            data_points=len(summaries),
            has_enough_data=len(summaries) >= 3,
            message=None if len(summaries) >= 3 else "Пока данных мало для уверенного анализа.",
        ),
        summary=AnalyticsSummary(
            period_days=days,
            health_score=health_score,
            sleep_score=sleep_score,
            hydration_score=hydration_score,
            activity_score=activity_score,
            nutrition_score=nutrition_score,
            state_score=state_score,
        ),
        insights=build_insight_items(insights),
        recommendations=recommendations,
        trends=trends,
    )


def _format_int(value: float | int | None) -> int:
    if value is None:
        return 0
    return int(round(float(value)))


def _profile_dietary_rules(db: Session, user_id: int):
    from app.models.profile import UserProfile
    from app.llm.dietary_prompt import build_dietary_rules_block

    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    if not profile:
        return "", None
    rules = build_dietary_rules_block(
        is_vegetarian=profile.is_vegetarian,
        allergies_text=profile.allergies_text if profile.has_allergies else None,
    )
    return rules, profile


@router.post(
    "/chat",
    response_model=AIResponse,
    summary="Задать вопрос AI health assistant",
    description="Возвращает живой ответ на основе уже рассчитанной аналитики."
)
def ai_chat(
    payload: AIChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.ai.user_health_context import build_user_health_context_text

    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=payload.period_days,
    )

    health_context = build_user_health_context_text(
        db=db,
        user_id=current_user.id,
        period_days=payload.period_days,
    )
    dietary_rules, _ = _profile_dietary_rules(db, current_user.id)

    history = [
        {"role": msg.role, "content": msg.content}
        for msg in payload.history[-20:]
    ]

    service = HealthChatService()

    try:
        return service.generate_chat_answer(
            analytics=analytics,
            user_question=payload.question,
            user_health_context=health_context,
            history=history,
            dietary_rules=dietary_rules,
        )
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )

@router.post(
    "/chat/stream",
    summary="Задать вопрос AI (стриминг)",
    description="Возвращает потоковый ответ SSE или просто чанки."
)
def ai_chat_stream(
    payload: AIChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.ai.user_health_context import build_user_health_context_text

    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=payload.period_days,
    )

    health_context = build_user_health_context_text(
        db=db,
        user_id=current_user.id,
        period_days=payload.period_days,
    )
    dietary_rules, _ = _profile_dietary_rules(db, current_user.id)

    history = [
        {"role": msg.role, "content": msg.content}
        for msg in payload.history[-20:]
    ]

    service = HealthChatService()

    try:
        generator = service.generate_chat_stream(
            analytics=analytics,
            user_question=payload.question,
            user_health_context=health_context,
            history=history,
            dietary_rules=dietary_rules,
        )
        return StreamingResponse(generator, media_type="text/plain")
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )


@router.get(
    "/daily-brief",
    response_model=AIBriefResponse,
    summary="Получить daily brief",
    description="Возвращает краткое AI-резюме дня на основе аналитики."
)
def get_daily_brief(
    days: int = Query(default=3, ge=1, le=14, description="Период анализа"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    service = HealthChatService()

    try:
        return service.generate_daily_brief(analytics=analytics)
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )


@router.get(
    "/weekly-brief",
    response_model=AIBriefResponse,
    summary="Получить weekly brief",
    description="Возвращает недельное AI-резюме."
)
def get_weekly_brief(
    days: int = Query(default=7, ge=3, le=30, description="Период анализа"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    service = HealthChatService()

    try:
        return service.generate_weekly_brief(analytics=analytics)
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )


@router.post(
    "/explain-insight",
    response_model=AIResponse,
    summary="Объяснить инсайт",
    description="Дает человеческое объяснение выбранного инсайта."
)
def explain_insight(
    payload: AIExplainInsightRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=payload.period_days,
    )

    matching_titles = [item.title for item in analytics.insights]
    if payload.insight_title not in matching_titles:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Инсайт с таким заголовком не найден в текущем аналитическом контексте",
        )

    service = HealthChatService()

    try:
        return service.explain_insight(
            analytics=analytics,
            insight_title=payload.insight_title,
        )
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )


@router.get(
    "/recommendations",
    response_model=AIRecommendationsResponse,
    summary="Получить AI-рекомендации",
    description="Возвращает готовый список AI-рекомендаций для мобильного клиента."
)
def get_ai_recommendations(
    days: int = Query(default=7, ge=1, le=30, description="Период анализа"),
    include_resolved: bool = Query(
        default=False,
        description="Включать выполненные и cooldown-рекомендации в ответ",
    ),
    use_llm_tips: bool = Query(
        default=False,
        description="Генерировать personalized_tip через LLM (медленно; для мобильного — false)",
    ),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    merged_items = build_merged_recommendation_items(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    recommendations = build_dynamic_ai_recommendations(
        db=db,
        user_id=current_user.id,
        analytics=analytics,
        recommendation_items=merged_items,
        include_resolved=include_resolved,
        use_llm_tips=use_llm_tips,
    )

    return AIRecommendationsResponse(
        generated_at=datetime.now(timezone.utc),
        period_days=days,
        health_score=analytics.summary.health_score,
        recommendations=recommendations,
    )

@router.post(
    "/recognize-food",
    response_model=AiRecognizedFoodResponse,
    summary="Распознать еду по фото",
    description="Анализирует фото еды и возвращает список продуктов с КБЖУ.",
    dependencies=[Depends(ai_rate_limit)],
)
async def recognize_food(
    image: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    try:
        contents = await image.read()
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Ошибка чтения файла: {exc}"
        )

    if not contents:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Файл пустой",
        )
    if len(contents) > settings.FOOD_RECOGNITION_MAX_BYTES:
        limit_mb = settings.FOOD_RECOGNITION_MAX_BYTES // (1024 * 1024)
        raise HTTPException(
            status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE,
            detail=f"Фото больше {limit_mb} МБ — сожмите изображение",
        )
    ok, detail = validate_avatar_bytes(contents)
    if not ok:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=detail)

    base64_image = base64.b64encode(contents).decode("utf-8")

    dietary_rules, profile = _profile_dietary_rules(db, current_user.id)
    prompt = (
        "Распознай еду на этом фото. Оцени примерный вес каждой порции в граммах и рассчитай КБЖУ. "
        "Верни ответ строго в формате JSON: "
        "{\"items\": [{\"name\": \"Название\", \"grams\": 150, \"calories\": 200, \"protein\": 10.5, \"fat\": 5.0, \"carbs\": 20.0}]}."
    )
    if dietary_rules:
        prompt += f"\n\n{dietary_rules}"

    from app.llm.llm_client import LLMClient, LLMClientError
    from app.llm.dietary_prompt import filter_food_items
    client = LLMClient()

    try:
        # analyze_image блокирует поток на десятки секунд (requests к LLM),
        # поэтому уводим его из event loop
        raw_json = await anyio.to_thread.run_sync(
            functools.partial(client.analyze_image, base64_image=base64_image, prompt=prompt)
        )
        data = json.loads(raw_json)
        if profile:
            data["items"] = filter_food_items(
                data.get("items", []),
                is_vegetarian=profile.is_vegetarian,
                allergies_text=profile.allergies_text if profile.has_allergies else None,
            )
        return AiRecognizedFoodResponse(**data)
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка обработки ответа LLM: {exc}",
        )

@router.post(
    "/recognize-text-food",
    response_model=AiRecognizedFoodResponse,
    summary="Распознать еду по тексту",
    description="Анализирует текст (например, голосовой ввод) и возвращает список продуктов с КБЖУ."
)
def recognize_text_food(
    payload: AiRecognizeTextFoodRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    dietary_rules, profile = _profile_dietary_rules(db, current_user.id)
    prompt = (
        f"Распознай еду из следующего текста: '{payload.text}'. "
        "Оцени примерный вес каждой порции в граммах и рассчитай КБЖУ. "
        "Верни ответ строго в формате JSON: "
        "{\"items\": [{\"name\": \"Название\", \"grams\": 150, \"calories\": 200, \"protein\": 10.5, \"fat\": 5.0, \"carbs\": 20.0}]}."
    )
    if dietary_rules:
        prompt += f"\n\n{dietary_rules}"

    from app.llm.llm_client import LLMClient, LLMClientError
    from app.llm.dietary_prompt import filter_food_items
    client = LLMClient()

    try:
        raw_json = client.generate(prompt=prompt)
        data = json.loads(raw_json)
        if profile:
            data["items"] = filter_food_items(
                data.get("items", []),
                is_vegetarian=profile.is_vegetarian,
                allergies_text=profile.allergies_text if profile.has_allergies else None,
            )
        return AiRecognizedFoodResponse(**data)
    except LLMClientError as exc:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"LLM недоступна: {exc}",
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка обработки ответа LLM: {exc}",
        )

@router.get(
    "/meal_plan",
    response_model=MealPlanResponse,
    summary="Сгенерировать AI план питания на неделю",
    description="Создает персонализированный план питания с рецептами и списком покупок"
)
def get_meal_plan(
    days: int = Query(default=7, ge=1, le=14),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.ai.user_health_context import build_meal_plan_context
    from app.llm.meal_plan_diet_filter import apply_dietary_filters
    from app.llm.meal_plan_portions import build_grocery_list_from_days, enrich_meal_plan_days
    from app.models.profile import UserProfile

    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    analytics = build_analytics_context(db, current_user.id, days=7)
    health_context = build_meal_plan_context(db=db, user_id=current_user.id)
    is_vegetarian = profile.is_vegetarian is True if profile else False
    allergies_text = (
        profile.allergies_text
        if profile and profile.has_allergies and profile.allergies_text
        else None
    )
    service = HealthChatService()
    source = "llm"
    try:
        raw_json, source = service.generate_meal_plan(
            analytics,
            user_context=health_context,
            days=days,
            is_vegetarian=is_vegetarian,
            allergies_text=allergies_text,
        )
        from app.llm.json_utils import parse_llm_json

        try:
            data = parse_llm_json(raw_json)
            if not data.get("days"):
                raise ValueError("В ответе нет поля days")
        except (ValueError, json.JSONDecodeError):
            source = "fallback"
            data = json.loads(
                service._build_fallback_meal_plan(
                    analytics,
                    days=days,
                    user_context=health_context,
                    is_vegetarian=is_vegetarian,
                )
            )
        if profile:
            data = apply_dietary_filters(
                data,
                is_vegetarian=profile.is_vegetarian,
                allergies_text=profile.allergies_text,
            )
        data["days"] = enrich_meal_plan_days(data.get("days", []))
        data["grocery_list"] = build_grocery_list_from_days(data.get("days", []))
        return MealPlanResponse(
            generated_at=datetime.now(timezone.utc),
            days=data.get("days", []),
            grocery_list=data.get("grocery_list", []),
            source=source,
        )
    except Exception as exc:
        if settings.AI_FALLBACK_ENABLED:
            data = json.loads(
                service._build_fallback_meal_plan(
                    analytics,
                    days=days,
                    user_context=health_context,
                    is_vegetarian=is_vegetarian,
                )
            )
            if profile:
                data = apply_dietary_filters(
                    data,
                    is_vegetarian=profile.is_vegetarian,
                    allergies_text=profile.allergies_text,
                )
            data["days"] = enrich_meal_plan_days(data.get("days", []))
            data["grocery_list"] = build_grocery_list_from_days(data.get("days", []))
            return MealPlanResponse(
                generated_at=datetime.now(timezone.utc),
                days=data.get("days", []),
                grocery_list=data.get("grocery_list", []),
                source="fallback",
            )
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка генерации плана питания: {exc}"
        )


@router.get(
    "/status",
    response_model=AiStatusResponse,
    summary="Статус LLM (Ollama / OpenAI)",
)
def get_ai_status(_current_user: User = Depends(get_current_user)):
    from app.llm.llm_client import LLMClient

    client = LLMClient()
    available, message = client.check_availability()
    return AiStatusResponse(
        llm_enabled=settings.LLM_ENABLED,
        llm_provider=settings.LLM_PROVIDER,
        llm_model=settings.LLM_MODEL_NAME,
        llm_available=available,
        fallback_enabled=settings.AI_FALLBACK_ENABLED,
        message=message,
    )

@router.get(
    "/workout_plan",
    response_model=WorkoutPlanResponse,
    summary="Сгенерировать AI план тренировок на неделю",
    description="Создает персонализированный план тренировок на основе активности"
)
def get_workout_plan(
    days: int = Query(default=7, ge=1, le=14),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.ai.user_health_context import build_user_health_context_text

    analytics = build_analytics_context(db, current_user.id, days=14)
    health_context = build_user_health_context_text(db=db, user_id=current_user.id, period_days=14)
    dietary_rules, _ = _profile_dietary_rules(db, current_user.id)
    if dietary_rules:
        health_context = f"{health_context}\n\n{dietary_rules}"
    service = HealthChatService()
    try:
        raw_json = service.generate_workout_plan(analytics, user_context=health_context, days=days)
        data = json.loads(raw_json)
        return WorkoutPlanResponse(
            generated_at=datetime.now(timezone.utc),
            workouts=data.get("workouts", [])
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка генерации плана тренировок: {exc}"
        )

@router.get(
    "/dashboard-hints",
    response_model=DashboardHintsResponse,
    summary="Получить контекстные подсказки для дашборда",
    description="Собирает данные за сегодня/вчера и генерирует короткие полезные советы (hints) с помощью LLM."
)
def get_dashboard_hints(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.ai.user_health_context import build_user_health_context_text
    analytics = build_analytics_context(db, current_user.id, days=3)
    health_context = build_user_health_context_text(db=db, user_id=current_user.id, period_days=3)
    dietary_rules, profile = _profile_dietary_rules(db, current_user.id)
    
    service = HealthChatService()
    try:
        raw_json = service.generate_dashboard_hints(
            analytics,
            user_context=health_context,
            dietary_rules=dietary_rules,
        )
        try:
            data = json.loads(raw_json)
        except json.JSONDecodeError:
            data = json.loads(service._build_fallback_dashboard_hints(analytics))
        hints = [
            _shorten_dashboard_hint(h)
            for h in data.get("hints", [])
            if str(h).strip()
        ][:3]
        if profile:
            from app.llm.dietary_prompt import contains_meat_or_fish, profile_implies_vegetarian
            if profile_implies_vegetarian(profile.is_vegetarian, profile.allergies_text if profile.has_allergies else None):
                hints = [h for h in hints if not contains_meat_or_fish(str(h))]
        return DashboardHintsResponse(hints=hints)
    except Exception as exc:
        if settings.AI_FALLBACK_ENABLED:
            data = json.loads(service._build_fallback_dashboard_hints(analytics))
            hints = [
                _shorten_dashboard_hint(h)
                for h in data.get("hints", [])
                if str(h).strip()
            ][:3]
            return DashboardHintsResponse(hints=hints)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка генерации подсказок: {exc}"
        )

@router.get(
    "/proactive-tip",
    response_model=ProactiveTipResponse,
    summary="Получить проактивный AI совет",
    description="Генерирует короткий проактивный совет на основе текущего состояния пользователя для push-уведомления."
)
def get_proactive_tip(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from app.services.ai.user_health_context import build_user_health_context_text
    from app.schemas.ai import ProactiveTipResponse
    analytics = build_analytics_context(db, current_user.id, days=1)
    health_context = build_user_health_context_text(db=db, user_id=current_user.id, period_days=1)
    dietary_rules, profile = _profile_dietary_rules(db, current_user.id)
    
    service = HealthChatService()
    try:
        tip_text = service.generate_proactive_tip(
            analytics,
            user_context=health_context,
            dietary_rules=dietary_rules,
        )
        if profile:
            from app.llm.dietary_prompt import contains_meat_or_fish, profile_implies_vegetarian
            if profile_implies_vegetarian(profile.is_vegetarian, profile.allergies_text if profile.has_allergies else None):
                if contains_meat_or_fish(tip_text):
                    tip_text = "Добавьте овощи и бобовые — они дадут энергию без тяжести."
        return ProactiveTipResponse(
            tip=tip_text,
            generated_at=datetime.now(timezone.utc)
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка генерации совета: {exc}"
        )

@router.post(
    "/sleep-summary",
    response_model=SleepSummaryResponse,
    summary="Сгенерировать саммари по звукам сна",
    description="Принимает список записанных звуков сна и возвращает короткое саммари (сводку)."
)
def generate_sleep_summary(
    payload: SleepSummaryRequest,
    current_user: User = Depends(get_current_user),
):
    from app.services.ai.sleep_summary_service import generate_sleep_summary as build_summary

    try:
        return build_summary(payload.sounds)
    except Exception as exc:
        from app.llm.llm_client import LLMClientError

        if isinstance(exc, LLMClientError):
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail=f"LLM недоступна: {exc}",
            ) from exc
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Ошибка генерации саммари: {exc}",
        ) from exc
