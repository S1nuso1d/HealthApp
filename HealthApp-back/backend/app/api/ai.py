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
from app.models.profile import UserProfile
from app.models.saved_recommendation import SavedRecommendation
from app.models.user import User
from app.models.user_state import UserState
from app.llm.llm_client import LLMClient
from app.recommendations.recommendation_engine import RecommendationEngine
from app.schemas.ai import (
    AIBriefResponse,
    AIChatRequest,
    AIExplainInsightRequest,
    AIRecommendationItem,
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

    if summaries:
        avg_sleep_hours = sum(s.total_sleep_hours or 0 for s in summaries) / len(summaries)
        avg_water_ml = sum(s.total_water_ml or 0 for s in summaries) / len(summaries)
        avg_steps = sum(s.total_steps or 0 for s in summaries) / len(summaries)
        avg_caffeine_mg = sum(s.total_caffeine_mg or 0 for s in summaries) / len(summaries)
    else:
        avg_sleep_hours = 0.0
        avg_water_ml = 0.0
        avg_steps = 0.0
        avg_caffeine_mg = 0.0

    sleep_score = calculate_sleep_score(avg_sleep_hours)
    hydration_score = calculate_hydration_score(avg_water_ml)
    activity_score = calculate_activity_score(avg_steps)
    nutrition_score = calculate_nutrition_score(avg_caffeine_mg)
    state_score = calculate_state_score(db, user_id, start_date, end_date)

    health_score = clamp_score(
        (sleep_score + hydration_score + activity_score + nutrition_score + state_score) / 5
    )

    recommendations = RecommendationEngine.generate_recommendations(
        db=db,
        user_id=user_id,
    )

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
    )


def _format_int(value: float | int | None) -> int:
    if value is None:
        return 0
    return int(round(float(value)))


def _build_personalized_tip_with_llm(
    recommendation: AIRecommendationItem,
    metrics_context: dict[str, int],
) -> str | None:
    prompt = f"""
Сформируй короткий персональный совет на русском языке (1-2 предложения, без диагнозов).

Рекомендация:
- category: {recommendation.category}
- title: {recommendation.title}
- description: {recommendation.description}
- action: {recommendation.action or "нет"}

Контекст пользователя:
- health_score: {metrics_context["health_score"]}
- hydration_score: {metrics_context["hydration_score"]}
- sleep_score: {metrics_context["sleep_score"]}
- activity_score: {metrics_context["activity_score"]}
- nutrition_score: {metrics_context["nutrition_score"]}
- state_score: {metrics_context["state_score"]}

Ответ только текстом совета, без списков и markdown.
    """.strip()

    try:
        return LLMClient().generate(prompt=prompt, temperature=0.25)
    except LLMClientError:
        return None


def _build_dynamic_ai_recommendations(
    db: Session,
    user_id: int,
    analytics: AnalyticsResponse,
    include_resolved: bool = False,
) -> list[AIRecommendationItem]:
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    latest_summary = (
        db.query(DailyHealthSummary)
        .filter(DailyHealthSummary.user_id == user_id)
        .order_by(DailyHealthSummary.summary_date.desc())
        .first()
    )

    target_water_ml = _format_int(profile.target_water_ml if profile else None) or 2500
    target_sleep_hours = float(profile.target_sleep_hours if profile and profile.target_sleep_hours else 8.0)
    latest_water_ml = _format_int(latest_summary.total_water_ml if latest_summary else None)
    latest_sleep_hours = float(latest_summary.total_sleep_hours if latest_summary and latest_summary.total_sleep_hours else 0.0)
    latest_steps = _format_int(latest_summary.total_steps if latest_summary else None)

    metrics_context = {
        "health_score": analytics.summary.health_score,
        "hydration_score": analytics.summary.hydration_score,
        "sleep_score": analytics.summary.sleep_score,
        "activity_score": analytics.summary.activity_score,
        "nutrition_score": analytics.summary.nutrition_score,
        "state_score": analytics.summary.state_score,
    }

    existing_items = (
        db.query(SavedRecommendation)
        .filter(SavedRecommendation.user_id == user_id)
        .all()
    )
    saved_by_key = {(entry.category, entry.title): entry for entry in existing_items}
    now_utc = datetime.now(timezone.utc)
    cooldown_duration = timedelta(hours=12)

    items: list[AIRecommendationItem] = []
    for item in analytics.recommendations:
        rec_key = (item.category, item.title)
        saved = saved_by_key.get(rec_key)

        rec = AIRecommendationItem(
            category=item.category,
            title=item.title,
            description=item.description,
            priority=item.priority,
            status="active",
            confidence=item.confidence,
            action=item.action,
            related_insight_title=item.related_insight_title,
            related_insight_type=item.related_insight_type,
        )

        resolved_reason: str | None = None

        # Auto-resolve hydration recommendations when user's latest daily target is reached.
        if rec.category == "hydration":
            if latest_water_ml >= target_water_ml:
                rec.status = "resolved"
                resolved_reason = "Цель по воде на день достигнута."

            remaining_ml = max(target_water_ml - latest_water_ml, 0)
            rec.progress_label = f"Сегодня: {latest_water_ml}/{target_water_ml} мл"
            if rec.status == "active":
                rec.action = (
                    f"До дневной цели осталось около {remaining_ml} мл. "
                    "Добавь 1-2 приёма воды в ближайшие часы."
                )

        # Auto-resolve sleep recommendation when last day meets personal sleep target.
        if rec.category == "sleep":
            if latest_sleep_hours >= target_sleep_hours:
                rec.status = "resolved"
                resolved_reason = "Последний сон соответствует персональной цели."

            remaining_sleep = max(target_sleep_hours - latest_sleep_hours, 0.0)
            rec.progress_label = f"Последний сон: {latest_sleep_hours:.1f}/{target_sleep_hours:.1f} ч"
            if rec.status == "active":
                rec.action = (
                    f"До персональной цели сна не хватает ~{remaining_sleep:.1f} ч. "
                    "Смести отбой на 30-60 минут раньше."
                )

        # Auto-resolve activity recommendation for days with near-goal step count.
        if rec.category == "activity":
            if latest_steps >= 9000:
                rec.status = "resolved"
                resolved_reason = "Дневной ориентир по шагам достигнут."

            remaining_steps = max(9000 - latest_steps, 0)
            rec.progress_label = f"Сегодня: {latest_steps}/9000 шагов"
            if rec.status == "active":
                rec.action = (
                    f"Осталось примерно {remaining_steps} шагов до дневного ориентира. "
                    "Добавь 1-2 короткие прогулки."
                )

        # Cooldown logic: do not immediately return resolved recommendation back to active list.
        if rec.status == "active" and saved and saved.status == "resolved":
            if saved.created_at:
                saved_created_at = saved.created_at
                if saved_created_at.tzinfo is None:
                    saved_created_at = saved_created_at.replace(tzinfo=timezone.utc)
                if now_utc < (saved_created_at + cooldown_duration):
                    rec.status = "cooldown"
                    rec.progress_label = rec.progress_label or "Рекомендация временно скрыта после выполнения."

        if rec.status in {"resolved", "cooldown"}:
            if saved is None:
                saved = SavedRecommendation(
                    user_id=user_id,
                    analysis_run_id=None,
                    category=rec.category,
                    title=rec.title,
                    description=rec.description,
                    priority=rec.priority,
                    confidence=rec.confidence,
                    action=resolved_reason,
                    related_insight_type=rec.related_insight_type,
                    related_insight_title=rec.related_insight_title,
                    status="resolved" if rec.status == "resolved" else "cooldown",
                )
                db.add(saved)
            else:
                saved.status = "resolved" if rec.status == "resolved" else "cooldown"
                saved.action = resolved_reason or saved.action
                saved.description = rec.description
                saved.priority = rec.priority
                saved.confidence = rec.confidence

            if include_resolved:
                rec.personalized_tip = "Отличная работа! Эта рекомендация уже выполнена."
                items.append(rec)
            continue

        if saved:
            saved.status = "active"
            saved.description = rec.description
            saved.priority = rec.priority
            saved.confidence = rec.confidence
            saved.action = rec.action
        else:
            saved = SavedRecommendation(
                user_id=user_id,
                analysis_run_id=None,
                category=rec.category,
                title=rec.title,
                description=rec.description,
                priority=rec.priority,
                confidence=rec.confidence,
                action=rec.action,
                related_insight_type=rec.related_insight_type,
                related_insight_title=rec.related_insight_title,
                status="active",
            )
            db.add(saved)

        rec.personalized_tip = _build_personalized_tip_with_llm(
            recommendation=rec,
            metrics_context=metrics_context,
        )
        items.append(rec)

    db.commit()

    return items


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

    service = HealthChatService()

    try:
        return service.generate_chat_answer(
            analytics=analytics,
            user_question=payload.question,
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
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    recommendations = _build_dynamic_ai_recommendations(
        db=db,
        user_id=current_user.id,
        analytics=analytics,
        include_resolved=include_resolved,
    )

    return AIRecommendationsResponse(
        generated_at=datetime.now(timezone.utc),
        period_days=days,
        health_score=analytics.summary.health_score,
        recommendations=recommendations,
    )