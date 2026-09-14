import json
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.analysis_run import AnalysisRun
from app.models.daily_health_summary import DailyHealthSummary
from app.models.insight import Insight
from app.models.saved_recommendation import SavedRecommendation
from app.models.user import User
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
from app.services.analytics.analytics_compare_service import compare_analysis_runs
from app.services.analytics.daily_summary_service import DailySummaryService
from app.services.analytics.insight_service import InsightService
from app.services.health_score_service import compute_period_average_scores
from app.services.influence_factors_service import build_influence_factors
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/analytics", tags=["Analytics"])


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
    """Баллы за период.

    Единственный источник формул — `health_score_service`. Раньше здесь лежала
    своя копия, и балл питания считался по кофеину, а на дашборде — по калориям
    относительно цели: один и тот же показатель показывал разные числа.
    Параметр `summaries` больше не нужен для расчёта и оставлен для совместимости
    вызовов, которые уже загрузили сводки.
    """
    return compute_period_average_scores(
        db=db,
        user_id=user_id,
        start_date=start_date,
        end_date=end_date,
    )


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


@router.get(
    "/influence-factors",
    summary="Что влияет на твоё самочувствие",
    description=(
        "Найденные закономерности в виде факторов влияния: сила связи, на что влияет "
        "и сравнение «в дни с фактором» против «в дни без него»."
    ),
)
def get_influence_factors(
    days: int = Query(default=14, ge=7, le=90, description="Окно анализа в днях"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return build_influence_factors(db=db, user_id=current_user.id, period_days=days)


def _run_or_404(db: Session, user_id: int, run_id: int) -> AnalysisRun:
    run = (
        db.query(AnalysisRun)
        .filter(AnalysisRun.id == run_id, AnalysisRun.user_id == user_id)
        .first()
    )
    if run is None:
        raise HTTPException(status_code=404, detail="Запуск аналитики не найден")
    return run


def _items_for_run(db: Session, user_id: int, run_id: int) -> list[SavedRecommendation]:
    return (
        db.query(SavedRecommendation)
        .filter(
            SavedRecommendation.user_id == user_id,
            SavedRecommendation.analysis_run_id == run_id,
        )
        .all()
    )


@router.get(
    "/compare",
    summary="Сравнить два запуска аналитики",
    description=(
        "Показывает, что изменилось между двумя пересчётами: дельты по баллам, "
        "какие рекомендации ушли, появились или остались. "
        "Без параметров сравниваются два последних запуска."
    ),
)
def compare_runs(
    previous_run_id: int | None = Query(default=None, description="ID более раннего запуска"),
    current_run_id: int | None = Query(default=None, description="ID более позднего запуска"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if previous_run_id is None or current_run_id is None:
        recent = (
            db.query(AnalysisRun)
            .filter(AnalysisRun.user_id == current_user.id)
            # id как второй ключ: два пересчёта подряд получают одинаковый
            # created_at (в SQLite точность до секунды), и без него порядок
            # последних запусков не определён.
            .order_by(AnalysisRun.created_at.desc(), AnalysisRun.id.desc())
            .limit(2)
            .all()
        )
        if len(recent) < 2:
            raise HTTPException(
                status_code=409,
                detail="Нужно минимум два пересчёта аналитики, чтобы было что сравнивать",
            )
        current_run, previous_run = recent[0], recent[1]
    else:
        previous_run = _run_or_404(db, current_user.id, previous_run_id)
        current_run = _run_or_404(db, current_user.id, current_run_id)

    return compare_analysis_runs(
        previous_run=previous_run,
        current_run=current_run,
        previous_items=_items_for_run(db, current_user.id, previous_run.id),
        current_items=_items_for_run(db, current_user.id, current_run.id),
    )


@router.get(
    "/saved-recommendations",
    summary="Сохранённые рекомендации",
    description="Рекомендации, сохранённые при пересчёте аналитики, с их статусом.",
)
def get_saved_recommendations(
    status_filter: str | None = Query(
        default=None,
        alias="status",
        description="Фильтр по статусу: new, read, resolved",
    ),
    limit: int = Query(default=50, ge=1, le=200),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = db.query(SavedRecommendation).filter(
        SavedRecommendation.user_id == current_user.id
    )
    if status_filter:
        query = query.filter(SavedRecommendation.status == status_filter)

    items = query.order_by(SavedRecommendation.created_at.desc()).limit(limit).all()
    return [
        {
            "id": item.id,
            "analysis_run_id": item.analysis_run_id,
            "category": item.category,
            "title": item.title,
            "description": item.description,
            "priority": item.priority,
            "confidence": item.confidence,
            "action": item.action,
            "related_insight_title": item.related_insight_title,
            "status": item.status,
            "created_at": item.created_at,
        }
        for item in items
    ]


@router.patch(
    "/saved-recommendations/{recommendation_id}",
    summary="Изменить статус сохранённой рекомендации",
)
def update_saved_recommendation_status(
    recommendation_id: int,
    new_status: str = Query(alias="status", description="new, read или resolved"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    allowed = {"new", "read", "resolved"}
    if new_status not in allowed:
        raise HTTPException(
            status_code=422,
            detail=f"Статус должен быть одним из: {', '.join(sorted(allowed))}",
        )

    item = (
        db.query(SavedRecommendation)
        .filter(
            SavedRecommendation.id == recommendation_id,
            SavedRecommendation.user_id == current_user.id,
        )
        .first()
    )
    if item is None:
        raise HTTPException(status_code=404, detail="Рекомендация не найдена")

    item.status = new_status
    db.commit()
    return {"id": item.id, "status": item.status}