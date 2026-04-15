import json
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.daily_health_summary import DailyHealthSummary
from app.models.insight import Insight
from app.models.user import User
from app.models.user_state import UserState
from app.recommendations.recommendation_engine import RecommendationEngine
from app.schemas.analysis_run import AnalysisRunResponse
from app.schemas.analytics import (
    AnalyticsEvidence,
    AnalyticsMeta,
    AnalyticsResponse,
    AnalyticsSummary,
    InsightItem,
    RecommendationItem,
)
from app.services.analysis_run_service import AnalysisRunService
from app.services.analytics.daily_summary_service import DailySummaryService
from app.services.analytics.insight_service import InsightService
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/analytics", tags=["Analytics"])


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
    results: list[InsightItem] = []

    for item in db_insights:
        results.append(
            InsightItem(
                category=item.category,
                title=item.title,
                description=item.description,
                confidence=item.confidence,
                impact=item.impact,
                severity=item.severity,
                evidence=parse_evidence(item),
            )
        )

    return results


def calculate_scores(
    db: Session,
    user_id: int,
    start_date: date,
    end_date: date,
    summaries: list[DailyHealthSummary],
) -> dict[str, int]:
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

    return {
        "health_score": health_score,
        "sleep_score": sleep_score,
        "hydration_score": hydration_score,
        "activity_score": activity_score,
        "nutrition_score": nutrition_score,
        "state_score": state_score,
    }


@router.post(
    "/rebuild",
    response_model=AnalysisRunResponse,
    summary="Пересчитать аналитику пользователя",
    description="Пересчитывает summary, insights, recommendations, smart triggers и сохраняет историю запуска."
)
def rebuild_my_analytics(
    days: int = Query(default=7, ge=3, le=60, description="Количество дней для пересчета"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    summaries = DailySummaryService.rebuild_summaries_for_last_days(
        db=db,
        user_id=current_user.id,
        days=days
    )

    insights = InsightService.rebuild_insights_for_user(
        db=db,
        user_id=current_user.id,
        window_days=days
    )

    recommendations = RecommendationEngine.generate_recommendations(
        db=db,
        user_id=current_user.id,
    )

    smart_result = generate_smart_triggers_and_reminders(
        db=db,
        user_id=current_user.id,
        period_days=days,
    )

    end_date = date.today()
    start_date = end_date - timedelta(days=days - 1)

    scores = calculate_scores(
        db=db,
        user_id=current_user.id,
        start_date=start_date,
        end_date=end_date,
        summaries=summaries,
    )

    run = AnalysisRunService.create_run(
        db=db,
        user_id=current_user.id,
        period_days=days,
        summaries_count=len(summaries),
        insights_count=len(insights),
        recommendations_count=len(recommendations),
        smart_triggers_count=int(smart_result.get("triggers_created", 0)),
        health_score=scores["health_score"],
        sleep_score=scores["sleep_score"],
        hydration_score=scores["hydration_score"],
        activity_score=scores["activity_score"],
        nutrition_score=scores["nutrition_score"],
        state_score=scores["state_score"],
        status="completed",
    )

    return run


@router.get(
    "/overview",
    response_model=AnalyticsResponse,
    summary="Получить общую аналитику пользователя",
    description="Возвращает summary, инсайты и рекомендации за выбранный период."
)
def get_analytics_overview(
    days: int = Query(default=7, ge=3, le=60, description="Период анализа в днях"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    end_date = date.today()
    start_date = end_date - timedelta(days=days - 1)

    summaries = (
        db.query(DailyHealthSummary)
        .filter(
            DailyHealthSummary.user_id == current_user.id,
            DailyHealthSummary.summary_date >= start_date,
            DailyHealthSummary.summary_date <= end_date
        )
        .order_by(DailyHealthSummary.summary_date.asc())
        .all()
    )

    db_insights = (
        db.query(Insight)
        .filter(Insight.user_id == current_user.id)
        .order_by(Insight.created_at.desc())
        .all()
    )

    data_points = len(summaries)
    has_enough_data = data_points >= 3

    scores = calculate_scores(
        db=db,
        user_id=current_user.id,
        start_date=start_date,
        end_date=end_date,
        summaries=summaries,
    )

    insight_items = build_insight_items(db_insights)
    recommendation_items = RecommendationEngine.generate_recommendations(
        db=db,
        user_id=current_user.id,
    )

    message = None
    if not has_enough_data:
        message = "Пока данных мало для уверенного персонального анализа. Добавь еще несколько дней записей."

    return AnalyticsResponse(
        meta=AnalyticsMeta(
            generated_at=datetime.now(timezone.utc),
            start_date=start_date,
            end_date=end_date,
            data_points=data_points,
            has_enough_data=has_enough_data,
            message=message,
        ),
        summary=AnalyticsSummary(
            period_days=days,
            health_score=scores["health_score"],
            sleep_score=scores["sleep_score"],
            hydration_score=scores["hydration_score"],
            activity_score=scores["activity_score"],
            nutrition_score=scores["nutrition_score"],
            state_score=scores["state_score"],
        ),
        insights=insight_items,
        recommendations=recommendation_items,
    )


@router.get(
    "/insights",
    response_model=list[InsightItem],
    summary="Получить инсайты пользователя",
    description="Возвращает найденные аналитические инсайты пользователя."
)
def get_insights(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    db_insights = (
        db.query(Insight)
        .filter(Insight.user_id == current_user.id)
        .order_by(Insight.created_at.desc())
        .all()
    )
    return build_insight_items(db_insights)


@router.get(
    "/recommendations",
    response_model=list[RecommendationItem],
    summary="Получить рекомендации пользователя",
    description="Возвращает рекомендации, построенные на основе инсайтов."
)
def get_recommendations(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return RecommendationEngine.generate_recommendations(
        db=db,
        user_id=current_user.id,
    )


@router.get(
    "/runs",
    response_model=list[AnalysisRunResponse],
    summary="Получить историю аналитических запусков",
    description="Возвращает историю последних запусков аналитики пользователя."
)
def get_analysis_runs(
    limit: int = Query(default=20, ge=1, le=100, description="Количество записей"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return AnalysisRunService.get_runs_for_user(
        db=db,
        user_id=current_user.id,
        limit=limit,
    )