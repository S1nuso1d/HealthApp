from datetime import date, datetime, time, timedelta, timezone
from typing import Optional

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.daily_health_summary import DailyHealthSummary
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.models.user_state import UserState


def safe_mean(values: list[float | int | None]) -> Optional[float]:
    filtered = [float(v) for v in values if v is not None]
    if not filtered:
        return None
    return round(sum(filtered) / len(filtered), 2)


class DailySummaryService:
    @staticmethod
    def _get_day_range(target_date: date) -> tuple[datetime, datetime]:
        day_start = datetime.combine(target_date, time.min).replace(tzinfo=timezone.utc)
        day_end = day_start + timedelta(days=1)
        return day_start, day_end

    @staticmethod
    def rebuild_summary_for_day(
        db: Session,
        user_id: int,
        target_date: date
    ) -> DailyHealthSummary:
        day_start, day_end = DailySummaryService._get_day_range(target_date)

        # ---------------------------------------------------------
        # Загружаем данные за день
        # ---------------------------------------------------------
        sleep_records = (
            db.query(SleepRecord)
            .filter(
                SleepRecord.user_id == user_id,
                SleepRecord.sleep_end >= day_start,
                SleepRecord.sleep_end < day_end,
            )
            .all()
        )

        hydration_records = (
            db.query(HydrationRecord)
            .filter(
                HydrationRecord.user_id == user_id,
                HydrationRecord.record_time >= day_start,
                HydrationRecord.record_time < day_end,
            )
            .all()
        )

        meal_records = (
            db.query(MealRecord)
            .filter(
                MealRecord.user_id == user_id,
                MealRecord.meal_time >= day_start,
                MealRecord.meal_time < day_end,
            )
            .all()
        )

        activity_records = (
            db.query(ActivityRecord)
            .filter(
                ActivityRecord.user_id == user_id,
                ActivityRecord.start_time >= day_start,
                ActivityRecord.start_time < day_end,
            )
            .all()
        )

        state_records = (
            db.query(UserState)
            .filter(
                UserState.user_id == user_id,
                UserState.record_time >= day_start,
                UserState.record_time < day_end,
            )
            .all()
        )

        # ---------------------------------------------------------
        # Агрегация сна
        # ---------------------------------------------------------
        total_sleep_hours = round(
            sum(float(record.duration_hours or 0.0) for record in sleep_records),
            2
        )

        average_sleep_score = safe_mean(
            [record.quality_score for record in sleep_records]
        )

        # ---------------------------------------------------------
        # Агрегация гидратации
        # ---------------------------------------------------------
        total_water_ml = round(
            sum(
                float(record.amount_ml or 0.0) *
                (float(record.hydration_factor) if record.hydration_factor is not None else 1.0)
                for record in hydration_records
            ),
            2
        )

        # ---------------------------------------------------------
        # Агрегация питания
        # ---------------------------------------------------------
        total_calories = round(
            sum(float(record.calories or 0.0) for record in meal_records),
            2
        )

        total_caffeine_mg = round(
            sum(float(record.caffeine_mg or 0.0) for record in meal_records),
            2
        )

        # ---------------------------------------------------------
        # Агрегация активности
        # ---------------------------------------------------------
        total_steps = int(
            sum(int(record.steps or 0) for record in activity_records)
        )

        total_active_minutes = int(
            sum(int(record.duration_minutes or 0) for record in activity_records)
        )

        workouts_count = len(activity_records)

        # ---------------------------------------------------------
        # Агрегация субъективного состояния
        # ---------------------------------------------------------
        state_scores = []

        for state in state_records:
            values = []
            if state.mood is not None:
                values.append(float(state.mood))
            if state.energy is not None:
                values.append(float(state.energy))
            if state.stress is not None:
                # Стресс инвертируем: высокий стресс = хуже
                values.append(11.0 - float(state.stress))
            if state.focus is not None:
                values.append(float(state.focus))

            wellbeing_value = getattr(state, "wellbeing", None)
            if wellbeing_value is not None:
                values.append(float(wellbeing_value))

            if values:
                state_scores.append(sum(values) / len(values))

        total_state_score = safe_mean(state_scores)

        # ---------------------------------------------------------
        # Сбор notes
        # ---------------------------------------------------------
        notes_parts: list[str] = []

        if sleep_records:
            notes_parts.append(f"sleep_records={len(sleep_records)}")
        if hydration_records:
            notes_parts.append(f"hydration_records={len(hydration_records)}")
        if meal_records:
            notes_parts.append(f"meal_records={len(meal_records)}")
        if activity_records:
            notes_parts.append(f"activity_records={len(activity_records)}")
        if state_records:
            notes_parts.append(f"state_records={len(state_records)}")

        notes = "; ".join(notes_parts) if notes_parts else None

        # ---------------------------------------------------------
        # Создание или обновление summary
        # ---------------------------------------------------------
        summary = (
            db.query(DailyHealthSummary)
            .filter(
                DailyHealthSummary.user_id == user_id,
                DailyHealthSummary.summary_date == target_date,
            )
            .first()
        )

        if summary is None:
            summary = DailyHealthSummary(
                user_id=user_id,
                summary_date=target_date,
            )
            db.add(summary)

        summary.total_sleep_hours = float(total_sleep_hours)
        summary.average_sleep_score = average_sleep_score
        summary.total_water_ml = int(total_water_ml)
        summary.total_calories = float(total_calories)
        summary.total_caffeine_mg = float(total_caffeine_mg)
        summary.total_steps = total_steps
        summary.total_active_minutes = total_active_minutes
        summary.workouts_count = workouts_count
        summary.total_state_score = total_state_score
        summary.notes = notes

        db.commit()
        db.refresh(summary)
        return summary

    @staticmethod
    def rebuild_summaries_for_last_days(
        db: Session,
        user_id: int,
        days: int = 7
    ) -> list[DailyHealthSummary]:
        results: list[DailyHealthSummary] = []
        today = datetime.now(timezone.utc).date()

        for i in range(days):
            target_date = today - timedelta(days=i)
            summary = DailySummaryService.rebuild_summary_for_day(
                db=db,
                user_id=user_id,
                target_date=target_date,
            )
            results.append(summary)

        return list(reversed(results))