"""Экспорт данных пользователя за период.

Раньше экспорта на сервере не было вообще: клиент собирал текстовый файл из
закэшированного снимка дашборда, то есть фактически за один день. Здесь
отдаётся полная выгрузка за произвольный период — в JSON для отчётов и в CSV
для таблиц.
"""

from __future__ import annotations

import csv
import io
from datetime import date, datetime, time, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.activity import ActivityRecord
from app.models.daily_health_summary import DailyHealthSummary
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.models.user import User
from app.models.user_state import UserState
from app.services.health_score_service import compute_period_average_scores

router = APIRouter(prefix="/export", tags=["Export"])

MAX_PERIOD_DAYS = 366


def _period_bounds(days: int) -> tuple[date, date, datetime, datetime]:
    end_date = date.today()
    start_date = end_date - timedelta(days=days - 1)
    start_dt = datetime.combine(start_date, time.min).replace(tzinfo=timezone.utc)
    end_dt = datetime.combine(end_date, time.max).replace(tzinfo=timezone.utc)
    return start_date, end_date, start_dt, end_dt


def _collect(db: Session, user_id: int, days: int) -> dict:
    start_date, end_date, start_dt, end_dt = _period_bounds(days)

    sleeps = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == user_id,
            SleepRecord.sleep_start >= start_dt,
            SleepRecord.sleep_start <= end_dt,
        )
        .order_by(SleepRecord.sleep_start.asc())
        .all()
    )
    hydration = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.user_id == user_id,
            HydrationRecord.record_time >= start_dt,
            HydrationRecord.record_time <= end_dt,
        )
        .order_by(HydrationRecord.record_time.asc())
        .all()
    )
    meals = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == user_id,
            MealRecord.meal_time >= start_dt,
            MealRecord.meal_time <= end_dt,
        )
        .order_by(MealRecord.meal_time.asc())
        .all()
    )
    activities = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == user_id,
            ActivityRecord.start_time >= start_dt,
            ActivityRecord.start_time <= end_dt,
        )
        .order_by(ActivityRecord.start_time.asc())
        .all()
    )
    states = (
        db.query(UserState)
        .filter(
            UserState.user_id == user_id,
            UserState.record_time >= start_dt,
            UserState.record_time <= end_dt,
        )
        .order_by(UserState.record_time.asc())
        .all()
    )
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

    return {
        "start_date": start_date,
        "end_date": end_date,
        "profile": profile,
        "sleeps": sleeps,
        "hydration": hydration,
        "meals": meals,
        "activities": activities,
        "states": states,
        "summaries": summaries,
    }


@router.get(
    "/report",
    summary="Выгрузка данных за период в JSON",
    description=(
        "Полная выгрузка дневника, дневных сводок и средних баллов за период. "
        "Используется приложением для построения PDF-отчёта."
    ),
)
def export_report(
    days: int = Query(default=30, ge=1, le=MAX_PERIOD_DAYS, description="Период в днях"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    data = _collect(db, current_user.id, days)
    profile = data["profile"]

    scores = compute_period_average_scores(
        db=db,
        user_id=current_user.id,
        start_date=data["start_date"],
        end_date=data["end_date"],
    )

    return {
        "meta": {
            "generated_at": datetime.now(timezone.utc),
            "start_date": data["start_date"],
            "end_date": data["end_date"],
            "period_days": days,
        },
        "profile": None
        if profile is None
        else {
            "age": profile.age,
            "sex": profile.sex,
            "height_cm": profile.height_cm,
            "weight_kg": profile.weight_kg,
            "goal": profile.goal,
            "target_sleep_hours": profile.target_sleep_hours,
            "target_water_ml": profile.target_water_ml,
            "target_steps": profile.target_steps,
            "target_daily_calories": profile.target_daily_calories,
        },
        "scores": scores,
        "daily": [
            {
                "date": s.summary_date,
                "sleep_hours": s.total_sleep_hours,
                "water_ml": s.total_water_ml,
                "calories": s.total_calories,
                "caffeine_mg": s.total_caffeine_mg,
                "steps": s.total_steps,
                "active_minutes": s.total_active_minutes,
                "workouts": s.workouts_count,
                "state_score": s.total_state_score,
            }
            for s in data["summaries"]
        ],
        "sleeps": [
            {
                "sleep_start": s.sleep_start,
                "sleep_end": s.sleep_end,
                "duration_hours": s.duration_hours,
                "quality_score": s.quality_score,
                "deep_sleep_minutes": s.deep_sleep_minutes,
                "rem_sleep_minutes": s.rem_sleep_minutes,
                "source": s.source,
            }
            for s in data["sleeps"]
        ],
        "hydration": [
            {
                "record_time": h.record_time,
                "amount_ml": h.amount_ml,
                "drink_type": h.drink_type,
                "source": h.source,
            }
            for h in data["hydration"]
        ],
        "meals": [
            {
                "meal_time": m.meal_time,
                "meal_type": m.meal_type,
                "name": m.name,
                "calories": m.calories,
                "protein_g": m.protein_g,
                "fat_g": m.fat_g,
                "carbs_g": m.carbs_g,
            }
            for m in data["meals"]
        ],
        "activities": [
            {
                "start_time": a.start_time,
                "activity_type": a.activity_type,
                "duration_minutes": a.duration_minutes,
                "steps": a.steps,
                "distance_km": a.distance_km,
                "calories_burned": a.calories_burned,
                "avg_heart_rate": a.avg_heart_rate,
                "source": a.source,
            }
            for a in data["activities"]
        ],
        "states": [
            {
                "record_time": st.record_time,
                "mood": st.mood,
                "energy": st.energy,
                "stress": st.stress,
                "focus": st.focus,
                "wellbeing": st.wellbeing,
            }
            for st in data["states"]
        ],
    }


@router.get(
    "/csv",
    summary="Выгрузка дневных сводок за период в CSV",
    description="Одна строка на день — формат, который открывается в Excel и Google Sheets.",
)
def export_csv(
    days: int = Query(default=90, ge=1, le=MAX_PERIOD_DAYS, description="Период в днях"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    data = _collect(db, current_user.id, days)

    buffer = io.StringIO()
    # Разделитель «;» — Excel с русской локалью иначе кладёт всю строку в одну ячейку.
    writer = csv.writer(buffer, delimiter=";", lineterminator="\n")
    writer.writerow(
        [
            "date",
            "sleep_hours",
            "water_ml",
            "calories",
            "caffeine_mg",
            "steps",
            "active_minutes",
            "workouts",
            "state_score",
        ]
    )
    for s in data["summaries"]:
        writer.writerow(
            [
                s.summary_date.isoformat(),
                round(s.total_sleep_hours or 0, 2),
                int(s.total_water_ml or 0),
                round(s.total_calories or 0, 1),
                round(s.total_caffeine_mg or 0, 1),
                int(s.total_steps or 0),
                int(s.total_active_minutes or 0),
                int(s.workouts_count or 0),
                "" if s.total_state_score is None else round(s.total_state_score, 1),
            ]
        )

    filename = f"healthapp_{data['start_date']}_{data['end_date']}.csv"
    # BOM нужен, чтобы Excel распознал UTF-8 и не ломал кириллицу.
    payload = "\ufeff" + buffer.getvalue()
    return StreamingResponse(
        io.BytesIO(payload.encode("utf-8")),
        media_type="text/csv; charset=utf-8",
        headers={"Content-Disposition": f'attachment; filename="{filename}"'},
    )
