import json
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
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
)
from app.schemas.analytics import (
    AnalyticsEvidence,
    AnalyticsMeta,
    AnalyticsResponse,
    AnalyticsSummary,
    InsightItem,
)
from app.services.personalized_advisor import PersonalizedAdvisor

router = APIRouter(prefix="/ai", tags=["AI"])


def clamp_score(value: float) -> int:
    return max(0, min(100, round(value)))


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
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=payload.period_days,
    )

    from app.services.action_plan_sync_service import collect_today_goals

    goals = collect_today_goals(db, current_user.id)
    today = {
        "sleep_hours": goals.sleep_hours,
        "sleep_target": goals.sleep_target,
        "water_ml": goals.water_ml,
        "water_target": goals.water_target,
        "steps": goals.steps,
        "steps_target": goals.steps_target,
        "calories": goals.calories,
        "calories_target": goals.calories_target,
        "burned": goals.burned,
        "burn_target": goals.burn_target,
        "state_logged": goals.state_logged_today,
    }
    personal_hints = PersonalizedAdvisor.generate_recommendations(
        db, current_user.id, period_days=min(payload.period_days, 14)
    )[:4]

    service = HealthChatService()

    try:
        return service.generate_chat_answer(
            analytics=analytics,
            user_question=payload.question,
            today=today,
            personal_hints=personal_hints,
        )
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