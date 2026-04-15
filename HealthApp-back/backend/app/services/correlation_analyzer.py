import json
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from typing import Any

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.models.user_state import UserState


def safe_mean(values: list[float | int | None]) -> float:
    filtered = [float(v) for v in values if v is not None]
    if not filtered:
        return 0.0
    return sum(filtered) / len(filtered)


def clamp_confidence(value: float) -> float:
    return max(0.0, min(1.0, round(value, 2)))


def normalize_bool(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if value in (1, "1", "true", "True", "yes", "Yes"):
        return True
    return False


def datetime_to_hour_decimal(dt: datetime | None) -> float | None:
    if dt is None:
        return None
    return dt.hour + dt.minute / 60.0


def build_evidence(metric: str, value: float, unit: str | None = None, note: str | None = None) -> dict:
    return {
        "metric": metric,
        "value": round(float(value), 2),
        "unit": unit,
        "note": note,
    }


def estimate_confidence(
    sample_size_a: int,
    sample_size_b: int,
    effect_size: float,
    effect_threshold: float,
    base: float = 0.55,
) -> float:
    """
    Простая эвристика уверенности:
    - чем больше данных, тем выше confidence
    - чем сильнее эффект относительно порога, тем выше confidence
    """
    sample_score = min((sample_size_a + sample_size_b) / 10.0, 1.0) * 0.25
    if effect_threshold <= 0:
        effect_score = 0.0
    else:
        effect_score = min(abs(effect_size) / effect_threshold, 1.5) * 0.2

    return clamp_confidence(base + sample_score + effect_score)


class CorrelationAnalyzer:
    @staticmethod
    def analyze_correlations(
        db: Session,
        user_id: int,
        period_days: int = 14,
    ) -> list[dict]:
        """
        Возвращает список correlation-insights в виде словарей:
        {
            "insight_type": ...,
            "category": "correlation",
            "title": ...,
            "description": ...,
            "confidence": ...,
            "severity": ...,
            "impact": ...,
            "evidence_json": "...",
            "window_days": period_days,
        }
        """
        now = datetime.now(timezone.utc)
        period_start = now - timedelta(days=period_days)

        sleeps = (
            db.query(SleepRecord)
            .filter(
                SleepRecord.user_id == user_id,
                SleepRecord.sleep_end >= period_start,
            )
            .order_by(SleepRecord.sleep_end.asc())
            .all()
        )

        meals = (
            db.query(MealRecord)
            .filter(
                MealRecord.user_id == user_id,
                MealRecord.meal_time >= period_start,
            )
            .order_by(MealRecord.meal_time.asc())
            .all()
        )

        hydration_records = (
            db.query(HydrationRecord)
            .filter(
                HydrationRecord.user_id == user_id,
                HydrationRecord.record_time >= period_start,
            )
            .order_by(HydrationRecord.record_time.asc())
            .all()
        )

        activities = (
            db.query(ActivityRecord)
            .filter(
                ActivityRecord.user_id == user_id,
                ActivityRecord.start_time >= period_start,
            )
            .order_by(ActivityRecord.start_time.asc())
            .all()
        )

        states = (
            db.query(UserState)
            .filter(
                UserState.user_id == user_id,
                UserState.record_time >= period_start,
            )
            .order_by(UserState.record_time.asc())
            .all()
        )

        # --- Агрегация по дням ---
        daily_sleep: dict[str, dict] = {}
        daily_meals: dict[str, dict] = defaultdict(
            lambda: {
                "total_caffeine_mg": 0.0,
                "late_caffeine": False,
                "late_meal": False,
                "late_meal_count": 0,
                "meal_count": 0,
            }
        )
        daily_hydration: dict[str, dict] = defaultdict(
            lambda: {
                "hydration_ml": 0.0,
                "water_only_ml": 0.0,
                "late_drink": False,
            }
        )
        daily_activity: dict[str, dict] = defaultdict(
            lambda: {
                "total_duration_minutes": 0,
                "steps": 0,
                "high_evening_activity": False,
                "activity_count": 0,
            }
        )
        daily_state: dict[str, dict] = defaultdict(
            lambda: {
                "energy": [],
                "mood": [],
                "stress": [],
                "focus": [],
                "wellbeing": [],
            }
        )

        # Сон
        for s in sleeps:
            day_key = s.sleep_end.date().isoformat()
            daily_sleep[day_key] = {
                "duration_hours": float(s.duration_hours or 0.0),
                "quality_score": float(s.quality_score) if s.quality_score is not None else None,
                "sleep_start_hour": datetime_to_hour_decimal(s.sleep_start),
                "sleep_end_hour": datetime_to_hour_decimal(s.sleep_end),
            }

        # Питание
        for m in meals:
            day_key = m.meal_time.date().isoformat()
            daily_meals[day_key]["meal_count"] += 1
            daily_meals[day_key]["total_caffeine_mg"] += float(m.caffeine_mg or 0.0)

            is_late_caffeine = False
            if float(m.caffeine_mg or 0.0) >= 30:
                if m.meal_time.hour >= 16:
                    is_late_caffeine = True
                if m.minutes_before_sleep is not None and m.minutes_before_sleep < 360:
                    is_late_caffeine = True

            if is_late_caffeine:
                daily_meals[day_key]["late_caffeine"] = True

            is_late_meal = normalize_bool(getattr(m, "is_late_meal", False))
            if m.minutes_before_sleep is not None and m.minutes_before_sleep < 120:
                is_late_meal = True

            if is_late_meal:
                daily_meals[day_key]["late_meal"] = True
                daily_meals[day_key]["late_meal_count"] += 1

        # Гидратация
        for h in hydration_records:
            day_key = h.record_time.date().isoformat()
            hydration_factor = float(h.hydration_factor) if h.hydration_factor is not None else 1.0
            effective_ml = float(h.amount_ml or 0.0) * hydration_factor

            daily_hydration[day_key]["hydration_ml"] += effective_ml

            if (h.drink_type or "").lower() == "water":
                daily_hydration[day_key]["water_only_ml"] += float(h.amount_ml or 0.0)

            if normalize_bool(getattr(h, "is_late_drink", False)):
                daily_hydration[day_key]["late_drink"] = True

        # Активность
        for a in activities:
            day_key = a.start_time.date().isoformat()
            daily_activity[day_key]["activity_count"] += 1
            daily_activity[day_key]["total_duration_minutes"] += int(a.duration_minutes or 0)
            daily_activity[day_key]["steps"] += int(a.steps or 0)

            is_high_evening = False
            if normalize_bool(getattr(a, "is_evening_activity", False)) and (a.intensity or "").lower() == "high":
                is_high_evening = True

            if a.minutes_before_sleep is not None and a.minutes_before_sleep < 180 and (a.intensity or "").lower() == "high":
                is_high_evening = True

            if is_high_evening:
                daily_activity[day_key]["high_evening_activity"] = True

        # Субъективное состояние
        for st in states:
            day_key = st.record_time.date().isoformat()
            if st.energy is not None:
                daily_state[day_key]["energy"].append(float(st.energy))
            if st.mood is not None:
                daily_state[day_key]["mood"].append(float(st.mood))
            if st.stress is not None:
                daily_state[day_key]["stress"].append(float(st.stress))
            if st.focus is not None:
                daily_state[day_key]["focus"].append(float(st.focus))

            wellbeing_value = getattr(st, "wellbeing", None)
            if wellbeing_value is not None:
                daily_state[day_key]["wellbeing"].append(float(wellbeing_value))

        # Нормализованные дневные таблицы
        all_days = sorted(
            set(daily_sleep.keys())
            | set(daily_meals.keys())
            | set(daily_hydration.keys())
            | set(daily_activity.keys())
            | set(daily_state.keys())
        )

        day_rows: list[dict] = []
        for day_key in all_days:
            sleep_data = daily_sleep.get(day_key, {})
            meal_data = daily_meals.get(day_key, {})
            hydration_data = daily_hydration.get(day_key, {})
            activity_data = daily_activity.get(day_key, {})
            state_data = daily_state.get(day_key, {})

            row = {
                "day": day_key,
                "sleep_duration": sleep_data.get("duration_hours", 0.0),
                "sleep_quality": sleep_data.get("quality_score"),
                "sleep_start_hour": sleep_data.get("sleep_start_hour"),
                "late_caffeine": meal_data.get("late_caffeine", False),
                "total_caffeine_mg": meal_data.get("total_caffeine_mg", 0.0),
                "late_meal": meal_data.get("late_meal", False),
                "late_meal_count": meal_data.get("late_meal_count", 0),
                "hydration_ml": hydration_data.get("hydration_ml", 0.0),
                "water_only_ml": hydration_data.get("water_only_ml", 0.0),
                "late_drink": hydration_data.get("late_drink", False),
                "activity_minutes": activity_data.get("total_duration_minutes", 0),
                "steps": activity_data.get("steps", 0),
                "high_evening_activity": activity_data.get("high_evening_activity", False),
                "energy": safe_mean(state_data.get("energy", [])),
                "mood": safe_mean(state_data.get("mood", [])),
                "stress": safe_mean(state_data.get("stress", [])),
                "focus": safe_mean(state_data.get("focus", [])),
                "wellbeing": safe_mean(state_data.get("wellbeing", [])),
            }
            day_rows.append(row)

        if len(day_rows) < 4:
            return []

        insights: list[dict] = []

        # ------------------------------------------------------------------
        # 1. Поздний кофеин -> ухудшение сна
        # ------------------------------------------------------------------
        late_caffeine_days = [d for d in day_rows if d["late_caffeine"]]
        normal_caffeine_days = [d for d in day_rows if not d["late_caffeine"]]

        if len(late_caffeine_days) >= 2 and len(normal_caffeine_days) >= 2:
            avg_sleep_late_caffeine = safe_mean([d["sleep_duration"] for d in late_caffeine_days])
            avg_sleep_normal = safe_mean([d["sleep_duration"] for d in normal_caffeine_days])

            avg_sleep_start_late = safe_mean([d["sleep_start_hour"] for d in late_caffeine_days])
            avg_sleep_start_normal = safe_mean([d["sleep_start_hour"] for d in normal_caffeine_days])

            duration_diff = avg_sleep_late_caffeine - avg_sleep_normal
            start_diff = avg_sleep_start_late - avg_sleep_start_normal

            if duration_diff <= -0.5 or start_diff >= 0.75:
                confidence = estimate_confidence(
                    len(late_caffeine_days),
                    len(normal_caffeine_days),
                    max(abs(duration_diff), abs(start_diff)),
                    0.5,
                    base=0.62,
                )

                severity = "high" if duration_diff <= -1.0 or start_diff >= 1.5 else "medium"

                evidence = [
                    build_evidence(
                        "avg_sleep_hours_with_late_caffeine",
                        avg_sleep_late_caffeine,
                        "hours",
                        "Средняя длительность сна в дни с поздним кофеином",
                    ),
                    build_evidence(
                        "avg_sleep_hours_without_late_caffeine",
                        avg_sleep_normal,
                        "hours",
                        "Средняя длительность сна в дни без позднего кофеина",
                    ),
                    build_evidence(
                        "avg_sleep_start_with_late_caffeine",
                        avg_sleep_start_late,
                        "hour_of_day",
                        "Среднее время начала сна в дни с поздним кофеином",
                    ),
                    build_evidence(
                        "avg_sleep_start_without_late_caffeine",
                        avg_sleep_start_normal,
                        "hour_of_day",
                        "Среднее время начала сна в дни без позднего кофеина",
                    ),
                ]

                insights.append(
                    {
                        "insight_type": "late_caffeine_sleep_impact",
                        "category": "correlation",
                        "title": "Поздний кофеин связан с ухудшением сна",
                        "description": (
                            f"В дни, когда кофеин был поздно, длительность сна была в среднем "
                            f"на {abs(round(duration_diff, 2))} ч меньше "
                            f"или засыпание происходило позже на {round(start_diff, 2)} ч."
                        ),
                        "confidence": confidence,
                        "severity": severity,
                        "impact": "negative",
                        "evidence_json": json.dumps(evidence, ensure_ascii=False),
                        "window_days": period_days,
                    }
                )

        # ------------------------------------------------------------------
        # 2. Поздний прием пищи -> ухудшение сна
        # ------------------------------------------------------------------
        late_meal_days = [d for d in day_rows if d["late_meal"]]
        normal_meal_days = [d for d in day_rows if not d["late_meal"]]

        if len(late_meal_days) >= 2 and len(normal_meal_days) >= 2:
            avg_sleep_late_meal = safe_mean([d["sleep_duration"] for d in late_meal_days])
            avg_sleep_normal_meal = safe_mean([d["sleep_duration"] for d in normal_meal_days])

            avg_start_late_meal = safe_mean([d["sleep_start_hour"] for d in late_meal_days])
            avg_start_normal_meal = safe_mean([d["sleep_start_hour"] for d in normal_meal_days])

            duration_diff = avg_sleep_late_meal - avg_sleep_normal_meal
            start_diff = avg_start_late_meal - avg_start_normal_meal

            if duration_diff <= -0.4 or start_diff >= 0.5:
                confidence = estimate_confidence(
                    len(late_meal_days),
                    len(normal_meal_days),
                    max(abs(duration_diff), abs(start_diff)),
                    0.4,
                    base=0.58,
                )

                evidence = [
                    build_evidence(
                        "avg_sleep_hours_with_late_meal",
                        avg_sleep_late_meal,
                        "hours",
                        "Средняя длительность сна в дни с поздним приемом пищи",
                    ),
                    build_evidence(
                        "avg_sleep_hours_without_late_meal",
                        avg_sleep_normal_meal,
                        "hours",
                        "Средняя длительность сна в дни без позднего приема пищи",
                    ),
                ]

                insights.append(
                    {
                        "insight_type": "late_meal_sleep_impact",
                        "category": "correlation",
                        "title": "Поздний прием пищи может ухудшать сон",
                        "description": (
                            f"В дни с поздним приемом пищи сон был короче примерно на "
                            f"{abs(round(duration_diff, 2))} ч или начало сна смещалось позже."
                        ),
                        "confidence": confidence,
                        "severity": "medium",
                        "impact": "negative",
                        "evidence_json": json.dumps(evidence, ensure_ascii=False),
                        "window_days": period_days,
                    }
                )

        # ------------------------------------------------------------------
        # 3. Низкая гидратация -> низкая энергия
        # ------------------------------------------------------------------
        low_hydration_days = [d for d in day_rows if d["hydration_ml"] < 1600]
        good_hydration_days = [d for d in day_rows if d["hydration_ml"] >= 1600]

        if len(low_hydration_days) >= 2 and len(good_hydration_days) >= 2:
            avg_energy_low_hydration = safe_mean([d["energy"] for d in low_hydration_days if d["energy"] > 0])
            avg_energy_good_hydration = safe_mean([d["energy"] for d in good_hydration_days if d["energy"] > 0])

            energy_diff = avg_energy_good_hydration - avg_energy_low_hydration

            if avg_energy_low_hydration > 0 and avg_energy_good_hydration > 0 and energy_diff >= 1.0:
                confidence = estimate_confidence(
                    len(low_hydration_days),
                    len(good_hydration_days),
                    energy_diff,
                    1.0,
                    base=0.60,
                )

                evidence = [
                    build_evidence(
                        "avg_energy_low_hydration",
                        avg_energy_low_hydration,
                        "score_1_10",
                        "Средняя энергия в дни с низкой гидратацией",
                    ),
                    build_evidence(
                        "avg_energy_good_hydration",
                        avg_energy_good_hydration,
                        "score_1_10",
                        "Средняя энергия в дни с лучшей гидратацией",
                    ),
                ]

                insights.append(
                    {
                        "insight_type": "low_hydration_low_energy",
                        "category": "correlation",
                        "title": "Низкая гидратация связана с более низкой энергией",
                        "description": (
                            f"В дни с гидратацией ниже 1600 мл уровень энергии был ниже примерно "
                            f"на {round(energy_diff, 2)} балла."
                        ),
                        "confidence": confidence,
                        "severity": "medium",
                        "impact": "negative",
                        "evidence_json": json.dumps(evidence, ensure_ascii=False),
                        "window_days": period_days,
                    }
                )

        # ------------------------------------------------------------------
        # 4. Короткий сон -> низкая энергия
        # ------------------------------------------------------------------
        short_sleep_days = [d for d in day_rows if d["sleep_duration"] < 6.5]
        normal_sleep_days = [d for d in day_rows if d["sleep_duration"] >= 6.5]

        if len(short_sleep_days) >= 2 and len(normal_sleep_days) >= 2:
            avg_energy_short_sleep = safe_mean([d["energy"] for d in short_sleep_days if d["energy"] > 0])
            avg_energy_normal_sleep = safe_mean([d["energy"] for d in normal_sleep_days if d["energy"] > 0])

            energy_diff = avg_energy_normal_sleep - avg_energy_short_sleep

            if avg_energy_short_sleep > 0 and avg_energy_normal_sleep > 0 and energy_diff >= 1.0:
                confidence = estimate_confidence(
                    len(short_sleep_days),
                    len(normal_sleep_days),
                    energy_diff,
                    1.0,
                    base=0.65,
                )

                severity = "high" if energy_diff >= 2.0 else "medium"

                evidence = [
                    build_evidence(
                        "avg_energy_short_sleep",
                        avg_energy_short_sleep,
                        "score_1_10",
                        "Средняя энергия в дни с коротким сном",
                    ),
                    build_evidence(
                        "avg_energy_normal_sleep",
                        avg_energy_normal_sleep,
                        "score_1_10",
                        "Средняя энергия в дни с более длинным сном",
                    ),
                ]

                insights.append(
                    {
                        "insight_type": "short_sleep_low_energy",
                        "category": "correlation",
                        "title": "Короткий сон связан со снижением энергии",
                        "description": (
                            f"В дни, когда сон был короче 6.5 часов, энергия была ниже примерно "
                            f"на {round(energy_diff, 2)} балла."
                        ),
                        "confidence": confidence,
                        "severity": severity,
                        "impact": "negative",
                        "evidence_json": json.dumps(evidence, ensure_ascii=False),
                        "window_days": period_days,
                    }
                )

        # ------------------------------------------------------------------
        # 5. Поздняя интенсивная активность -> ухудшение сна
        # ------------------------------------------------------------------
        evening_high_days = [d for d in day_rows if d["high_evening_activity"]]
        normal_activity_days = [d for d in day_rows if not d["high_evening_activity"]]

        if len(evening_high_days) >= 2 and len(normal_activity_days) >= 2:
            avg_sleep_evening_high = safe_mean([d["sleep_duration"] for d in evening_high_days])
            avg_sleep_normal = safe_mean([d["sleep_duration"] for d in normal_activity_days])

            avg_start_evening_high = safe_mean([d["sleep_start_hour"] for d in evening_high_days])
            avg_start_normal = safe_mean([d["sleep_start_hour"] for d in normal_activity_days])

            duration_diff = avg_sleep_evening_high - avg_sleep_normal
            start_diff = avg_start_evening_high - avg_start_normal

            if duration_diff <= -0.4 or start_diff >= 0.5:
                confidence = estimate_confidence(
                    len(evening_high_days),
                    len(normal_activity_days),
                    max(abs(duration_diff), abs(start_diff)),
                    0.4,
                    base=0.58,
                )

                evidence = [
                    build_evidence(
                        "avg_sleep_hours_evening_high_activity",
                        avg_sleep_evening_high,
                        "hours",
                        "Средняя длительность сна в дни с поздней интенсивной активностью",
                    ),
                    build_evidence(
                        "avg_sleep_hours_without_evening_high_activity",
                        avg_sleep_normal,
                        "hours",
                        "Средняя длительность сна в дни без поздней интенсивной активности",
                    ),
                ]

                insights.append(
                    {
                        "insight_type": "evening_high_activity_sleep_impact",
                        "category": "correlation",
                        "title": "Поздняя интенсивная активность может мешать сну",
                        "description": (
                            f"В дни с поздней интенсивной активностью сон был короче или начинался позже."
                        ),
                        "confidence": confidence,
                        "severity": "medium",
                        "impact": "negative",
                        "evidence_json": json.dumps(evidence, ensure_ascii=False),
                        "window_days": period_days,
                    }
                )

        # ------------------------------------------------------------------
        # 6. Хорошая гидратация + активность -> лучшая энергия
        # ------------------------------------------------------------------
        strong_days = [
            d for d in day_rows
            if d["hydration_ml"] >= 1800 and d["activity_minutes"] >= 30
        ]
        weak_days = [
            d for d in day_rows
            if d["hydration_ml"] < 1800 and d["activity_minutes"] < 30
        ]

        if len(strong_days) >= 2 and len(weak_days) >= 2:
            avg_energy_strong = safe_mean([d["energy"] for d in strong_days if d["energy"] > 0])
            avg_energy_weak = safe_mean([d["energy"] for d in weak_days if d["energy"] > 0])

            energy_diff = avg_energy_strong - avg_energy_weak

            if avg_energy_strong > 0 and avg_energy_weak > 0 and energy_diff >= 1.0:
                confidence = estimate_confidence(
                    len(strong_days),
                    len(weak_days),
                    energy_diff,
                    1.0,
                    base=0.57,
                )

                evidence = [
                    build_evidence(
                        "avg_energy_good_hydration_and_activity",
                        avg_energy_strong,
                        "score_1_10",
                        "Средняя энергия в дни с хорошей гидратацией и активностью",
                    ),
                    build_evidence(
                        "avg_energy_low_hydration_and_low_activity",
                        avg_energy_weak,
                        "score_1_10",
                        "Средняя энергия в дни со слабой гидратацией и низкой активностью",
                    ),
                ]

                insights.append(
                    {
                        "insight_type": "hydration_activity_energy_positive",
                        "category": "correlation",
                        "title": "Гидратация и активность связаны с более высокой энергией",
                        "description": (
                            f"В дни с лучшей гидратацией и хотя бы умеренной активностью энергия была выше "
                            f"примерно на {round(energy_diff, 2)} балла."
                        ),
                        "confidence": confidence,
                        "severity": "low",
                        "impact": "positive",
                        "evidence_json": json.dumps(evidence, ensure_ascii=False),
                        "window_days": period_days,
                    }
                )

        return insights