import json
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.llm.health_chat_service import HealthChatService
from app.models.action_plan import ActionPlan
from app.models.daily_health_summary import DailyHealthSummary
from app.models.insight import Insight
from app.models.user import User
from app.models.user_state import UserState
from app.recommendations.recommendation_engine import RecommendationEngine
from app.schemas.action_plan import ActionPlanResponse
from app.schemas.ai import AIBriefResponse
from app.schemas.analytics import (
    AnalyticsEvidence,
    AnalyticsMeta,
    AnalyticsResponse,
    AnalyticsSummary,
    InsightItem,
)
from app.schemas.dashboard import (
    DashboardHomeResponse,
    SmartReminderItem,
    SmartTriggerItem,
)
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders


router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


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


@router.get(
    "/home",
    response_model=DashboardHomeResponse,
    summary="Главный экран приложения",
    description="Возвращает все данные, нужные для главного экрана."
)
def get_dashboard_home(
    days: int = Query(default=7, ge=1, le=30),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    action_plan = (
        db.query(ActionPlan)
        .filter(ActionPlan.user_id == current_user.id)
        .order_by(ActionPlan.created_at.desc())
        .all()
    )

    trigger_result = generate_smart_triggers_and_reminders(
        db=db,
        user_id=current_user.id,
        period_days=days,
    )

    smart_triggers = [
        SmartTriggerItem(
            type=item.get("type", "unknown"),
            title=item.get("title", ""),
            description=item.get("description", ""),
            severity=item.get("severity", "low"),
            confidence=item.get("confidence"),
        )
        for item in trigger_result.get("triggers", [])
    ]

    smart_reminders = [
        SmartReminderItem(
            type=item.get("type", "unknown"),
            title=item.get("title", ""),
            message=item.get("message", ""),
            recommended_time=item.get("recommended_time"),
        )
        for item in trigger_result.get("reminders", [])
    ]

    daily_brief: AIBriefResponse | None = None
    try:
        service = HealthChatService()
        daily_brief = service.generate_daily_brief(analytics=analytics)
    except Exception:
        daily_brief = None

    return DashboardHomeResponse(
        analytics=analytics,
        action_plan=action_plan,
        daily_brief=daily_brief,
        smart_triggers=smart_triggers,
        smart_reminders=smart_reminders,
    )