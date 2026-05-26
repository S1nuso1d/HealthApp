"""Сводные тренды пользователя за период (для аналитики и инсайтов)."""

from __future__ import annotations

from datetime import date, timedelta
from statistics import mean

from sqlalchemy.orm import Session

from app.models.daily_health_summary import DailyHealthSummary
from app.models.profile import UserProfile
from app.schemas.analytics import AnalyticsTrends


def _avg(values: list[float]) -> float | None:
    if not values:
        return None
    return round(mean(values), 2)


def compute_user_trends(
    db: Session,
    user_id: int,
    days: int = 7,
) -> AnalyticsTrends:
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

    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    target_water = int(profile.target_water_ml if profile and profile.target_water_ml else 2500)
    target_sleep = float(profile.target_sleep_hours if profile and profile.target_sleep_hours else 8.0)
    target_steps = int(profile.target_steps if profile and profile.target_steps else 10_000)
    target_calories = int(profile.target_daily_calories if profile and profile.target_daily_calories else 2200)

    if not summaries:
        return AnalyticsTrends(days_with_data=0)

    mid = max(1, len(summaries) // 2)
    first_half = summaries[:mid]
    second_half = summaries[mid:]

    def _collect(group: list[DailyHealthSummary], attr: str) -> list[float]:
        out: list[float] = []
        for row in group:
            val = getattr(row, attr, None)
            if val is not None and float(val) > 0:
                out.append(float(val))
        return out

    sleep_all = _collect(summaries, "total_sleep_hours")
    water_all = _collect(summaries, "total_water_ml")
    steps_all = _collect(summaries, "total_steps")
    calories_all = _collect(summaries, "total_calories")

    sleep_first = _avg(_collect(first_half, "total_sleep_hours"))
    sleep_second = _avg(_collect(second_half, "total_sleep_hours"))
    water_first = _avg(_collect(first_half, "total_water_ml"))
    water_second = _avg(_collect(second_half, "total_water_ml"))
    steps_first = _avg(_collect(first_half, "total_steps"))
    steps_second = _avg(_collect(second_half, "total_steps"))

    def _day_goals_met(row: DailyHealthSummary) -> bool:
        sleep_ok = (row.total_sleep_hours or 0) >= target_sleep * 0.9
        water_ok = (row.total_water_ml or 0) >= target_water
        steps_ok = (row.total_steps or 0) >= target_steps
        nutrition_ok = (row.total_calories or 0) >= target_calories * 0.85
        return sleep_ok and water_ok and steps_ok and nutrition_ok

    goals_met = sum(1 for s in summaries if _day_goals_met(s))

    return AnalyticsTrends(
        avg_sleep_hours=_avg(sleep_all),
        avg_water_ml=_avg(water_all),
        avg_steps=_avg(steps_all),
        avg_calories=_avg(calories_all),
        sleep_delta_vs_prev=(
            round(sleep_second - sleep_first, 2)
            if sleep_first is not None and sleep_second is not None
            else None
        ),
        water_delta_vs_prev=(
            round(water_second - water_first, 2)
            if water_first is not None and water_second is not None
            else None
        ),
        steps_delta_vs_prev=(
            round(steps_second - steps_first, 2)
            if steps_first is not None and steps_second is not None
            else None
        ),
        goals_met_days=goals_met,
        days_with_data=len(summaries),
    )


def build_trend_insight_candidates(trends: AnalyticsTrends) -> list[dict]:
    """Короткие инсайты по динамике метрик."""
    items: list[dict] = []

    if trends.sleep_delta_vs_prev is not None and trends.sleep_delta_vs_prev <= -0.75:
        items.append(
            {
                "insight_type": "sleep_trend_decline",
                "category": "sleep",
                "title": "Сон стал короче",
                "description": (
                    f"За вторую половину периода сон в среднем на {abs(trends.sleep_delta_vs_prev):.1f} ч "
                    "короче, чем в первой. Стоит сдвинуть отбой и снизить кофеин после 16:00."
                ),
                "confidence": 0.76,
                "severity": "medium",
                "impact": "negative",
            }
        )

    if trends.water_delta_vs_prev is not None and trends.water_delta_vs_prev <= -400:
        items.append(
            {
                "insight_type": "hydration_trend_decline",
                "category": "hydration",
                "title": "Вода: спад по неделе",
                "description": (
                    f"Средний объём воды снизился примерно на {abs(int(trends.water_delta_vs_prev))} мл "
                    "во второй половине периода. Поставьте 2–3 напоминания в дневнике гидратации."
                ),
                "confidence": 0.74,
                "severity": "medium",
                "impact": "negative",
            }
        )

    if trends.steps_delta_vs_prev is not None and trends.steps_delta_vs_prev >= 1500:
        items.append(
            {
                "insight_type": "activity_trend_up",
                "category": "activity",
                "title": "Активность растёт",
                "description": (
                    f"Шаги во второй половине периода выше примерно на {int(trends.steps_delta_vs_prev)} "
                    "в среднем — хороший импульс, закрепите режим короткими прогулками."
                ),
                "confidence": 0.7,
                "severity": "low",
                "impact": "positive",
            }
        )

    if trends.goals_met_days >= 3 and trends.days_with_data >= 5:
        items.append(
            {
                "insight_type": "goals_consistency",
                "category": "state",
                "title": "Стабильный прогресс по целям",
                "description": (
                    f"За период {trends.goals_met_days} из {trends.days_with_data} дней с данными "
                    "выполнены ключевые цели. Продолжайте в том же темпе."
                ),
                "confidence": 0.8,
                "severity": "low",
                "impact": "positive",
            }
        )

    return items
