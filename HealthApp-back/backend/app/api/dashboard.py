import json
from collections import defaultdict
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.action_plan import ActionPlan
from app.models.activity import ActivityRecord
from app.models.daily_health_summary import DailyHealthSummary
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.services.analytics.daily_summary_service import DailySummaryService
from app.models.insight import Insight
from app.models.user import User
from app.services.analytics.user_trends_service import compute_user_trends
from app.services.recommendation_orchestrator import build_merged_recommendation_items
from app.schemas.action_plan import ActionPlanResponse
from app.schemas.analytics import (
    AnalyticsEvidence,
    AnalyticsMeta,
    AnalyticsResponse,
    AnalyticsSummary,
    InsightItem,
)
from app.schemas.dashboard import (
    DashboardHomeResponse,
    GoalsCalendarDay,
    GoalsCalendarMealItem,
    GoalsCalendarResponse,
    SmartReminderItem,
    SmartTriggerItem,
)
from app.services.health_score_service import compute_period_average_scores, compute_today_scores
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders


router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


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

    today_scores = compute_today_scores(db=db, user_id=user_id)
    period_scores = compute_period_average_scores(
        db=db, user_id=user_id, start_date=start_date, end_date=end_date
    )
    sleep_score = today_scores["sleep_score"]
    hydration_score = today_scores["hydration_score"]
    activity_score = today_scores["activity_score"]
    nutrition_score = today_scores["nutrition_score"]
    state_score = today_scores["state_score"]
    health_score = today_scores["health_score"]
    _ = period_scores  # период остаётся для meta/insights

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
    from app.services.achievement_service import refresh_user_achievements

    refresh_user_achievements(db, current_user.id)

    analytics = build_analytics_context(
        db=db,
        user_id=current_user.id,
        days=days,
    )

    from app.services.action_plan_sync_service import ensure_daily_action_plan

    ensure_daily_action_plan(db, current_user.id, limit=5)

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

    return DashboardHomeResponse(
        analytics=analytics,
        action_plan=action_plan,
        daily_brief=None,
        smart_triggers=smart_triggers,
        smart_reminders=smart_reminders,
    )


def _progress(value: float, target: float) -> float:
    if target <= 0 or value <= 0:
        return 0.0
    return min(1.0, float(value) / float(target))


_WALK_TYPES = frozenset({"walk", "walking", "ходьба"})


def _utc_date(dt: datetime) -> date:
    if dt.tzinfo is not None:
        return dt.astimezone(timezone.utc).date()
    return dt.date()


def _steps_from_activities(records: list[ActivityRecord]) -> int:
    if not records:
        return 0
    walk_steps = max(
        (int(r.steps or 0) for r in records if (r.activity_type or "").lower() in _WALK_TYPES),
        default=0,
    )
    if walk_steps > 0:
        return walk_steps
    return max((int(r.steps or 0) for r in records), default=0)


def _empty_day_metrics() -> dict:
    return {
        "sleep": 0.0,
        "water": 0,
        "steps": 0,
        "burned": 0.0,
        "consumed": 0.0,
        "protein": 0.0,
        "fat": 0.0,
        "carbs": 0.0,
        "meals": [],
    }


def _aggregate_calendar_metrics(
    db: Session,
    user_id: int,
    start_date: date,
    end_date: date,
) -> dict[date, dict]:
    """
    Сон, вода, шаги, сожжённые и съеденные калории, БЖУ и приёмы пищи — из записей за период.
    Ключ — календарная дата (UTC по метке записи).
    """
    range_start, _ = DailySummaryService._get_day_range(start_date)
    _, range_end = DailySummaryService._get_day_range(end_date)

    metrics: dict[date, dict] = {}
    d = start_date
    while d <= end_date:
        metrics[d] = _empty_day_metrics()
        d += timedelta(days=1)

    for rec in (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == user_id,
            SleepRecord.sleep_end >= range_start,
            SleepRecord.sleep_end < range_end,
        )
        .all()
    ):
        day = _utc_date(rec.sleep_end)
        if day in metrics:
            metrics[day]["sleep"] = float(metrics[day]["sleep"]) + float(rec.duration_hours or 0.0)

    for rec in (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.user_id == user_id,
            HydrationRecord.record_time >= range_start,
            HydrationRecord.record_time < range_end,
        )
        .all()
    ):
        day = _utc_date(rec.record_time)
        if day in metrics:
            factor = float(rec.hydration_factor) if rec.hydration_factor is not None else 1.0
            metrics[day]["water"] = int(metrics[day]["water"]) + int(
                round(float(rec.amount_ml or 0) * factor)
            )

    activities_by_day: dict[date, list[ActivityRecord]] = defaultdict(list)
    for rec in (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= range_start,
            ActivityRecord.start_time < range_end,
        )
        .all()
    ):
        day = _utc_date(rec.start_time)
        if day in metrics:
            activities_by_day[day].append(rec)

    for day, recs in activities_by_day.items():
        metrics[day]["steps"] = _steps_from_activities(recs)
        metrics[day]["burned"] = round(sum(float(r.calories_burned or 0) for r in recs), 1)

    for rec in (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == user_id,
            MealRecord.meal_time >= range_start,
            MealRecord.meal_time < range_end,
        )
        .order_by(MealRecord.meal_time)
        .all()
    ):
        day = _utc_date(rec.meal_time)
        if day in metrics:
            row = metrics[day]
            row["consumed"] = float(row["consumed"]) + float(rec.calories or 0)
            row["protein"] = float(row["protein"]) + float(rec.protein_g or 0)
            row["fat"] = float(row["fat"]) + float(rec.fat_g or 0)
            row["carbs"] = float(row["carbs"]) + float(rec.carbs_g or 0)
            row["meals"].append(
                {
                    "name": rec.name or "Приём пищи",
                    "meal_type": rec.meal_type or "snack",
                    "meal_time": rec.meal_time.isoformat() if rec.meal_time else "",
                    "calories": float(rec.calories or 0),
                    "protein_g": float(rec.protein_g or 0),
                    "fat_g": float(rec.fat_g or 0),
                    "carbs_g": float(rec.carbs_g or 0),
                }
            )

    return metrics


@router.get(
    "/goals-calendar",
    response_model=GoalsCalendarResponse,
    summary="Календарь выполнения целей",
)
def get_goals_calendar(
    year: int | None = Query(default=None, ge=2020, le=2100),
    month: int | None = Query(default=None, ge=1, le=12),
    days: int = Query(default=35, ge=7, le=93),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from calendar import monthrange

    from app.models.profile import UserProfile

    today = date.today()
    if year is not None and month is not None:
        start_date = date(year, month, 1)
        last_day = monthrange(year, month)[1]
        end_date = date(year, month, last_day)
    else:
        end_date = today
        start_date = end_date - timedelta(days=days - 1)

    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()

    target_sleep = profile.target_sleep_hours if profile and profile.target_sleep_hours else 8.0
    target_water = int(profile.target_water_ml if profile and profile.target_water_ml else 2500)
    target_steps = int(profile.target_steps if profile and profile.target_steps else 10000)
    target_cal = int(profile.target_daily_calories if profile and profile.target_daily_calories else 2200)
    target_burned = max(400, int(target_steps * 0.05))

    raw_by_day = _aggregate_calendar_metrics(db, current_user.id, start_date, end_date)

    items: list[GoalsCalendarDay] = []
    d = start_date
    while d <= end_date:
        raw = raw_by_day.get(d, _empty_day_metrics())
        sleep_h = float(raw["sleep"])
        water = int(raw["water"])
        steps = int(raw["steps"])
        calories_burned = float(raw["burned"])
        cals_consumed = float(raw["consumed"])
        protein_g = float(raw["protein"])
        fat_g = float(raw["fat"])
        carbs_g = float(raw["carbs"])
        day_meals = raw["meals"]

        has_sleep = sleep_h > 0
        has_water = water > 0
        has_steps = steps > 0
        has_burned = calories_burned > 0
        has_consumed = cals_consumed > 0
        has_any = has_sleep or has_water or has_steps or has_burned or has_consumed

        sleep_prog = _progress(sleep_h, target_sleep) if has_sleep else 0.0
        water_prog = _progress(water, target_water) if has_water else 0.0
        steps_prog = _progress(steps, target_steps) if has_steps else 0.0
        burned_prog = _progress(calories_burned, target_burned) if has_burned else 0.0

        sleep_ok = sleep_h >= target_sleep * 0.9
        water_ok = water >= target_water
        steps_ok = steps >= target_steps
        nutrition_ok = calories_burned >= target_burned * 0.7 if target_burned > 0 else False
        all_ok = has_any and sleep_ok and water_ok and steps_ok and nutrition_ok

        items.append(
            GoalsCalendarDay(
                date=d.isoformat(),
                sleep_met=sleep_ok,
                hydration_met=water_ok,
                activity_met=steps_ok,
                nutrition_met=nutrition_ok,
                all_goals_met=all_ok,
                has_any_data=has_any,
                sleep_progress=round(sleep_prog, 3),
                hydration_progress=round(water_prog, 3),
                activity_progress=round(steps_prog, 3),
                nutrition_progress=round(burned_prog, 3),
                sleep_hours=round(sleep_h, 2),
                water_ml=water,
                steps=steps,
                calories=round(cals_consumed, 1),
                calories_burned=calories_burned,
                calories_consumed=round(cals_consumed, 1),
                protein_g=round(protein_g, 1),
                fat_g=round(fat_g, 1),
                carbs_g=round(carbs_g, 1),
                meals=[
                    GoalsCalendarMealItem(**m)
                    for m in day_meals[:30]
                ],
            )
        )
        d += timedelta(days=1)

    return GoalsCalendarResponse(days=items)