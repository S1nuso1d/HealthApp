"""
Импорт записей: пакетный JSON и текстовый CSV (поля через «;»).

Формат строк CSV (без заголовка, UTF-8, комментарии с #):
  hydration;record_time_iso;amount_ml
  sleep;sleep_start_iso;sleep_end_iso;quality_score;notes
  meal;meal_type;name;meal_time_iso;calories;protein_g;fat_g;carbs_g
  activity;activity_type;start_iso;end_iso;duration_minutes;steps
"""

from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.activity import ActivityRecord
from app.models.health_sample import HealthSample
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.models.user import User
from app.schemas.activity import ActivityCreate
from app.schemas.hydration import HydrationCreate
from app.schemas.import_schema import CsvImportBody, ImportBatchRequest, ImportBatchResponse
from app.schemas.meal import MealCreate
from app.schemas.sleep import SleepCreate
from app.services.analytics_sync import rebuild_user_analytics
from app.services.realtime_manager import realtime_manager
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/import", tags=["Import"])


def _sleep_record_for_user(user_id: int, sleep_data: SleepCreate) -> SleepRecord:
    duration_hours = round(
        (sleep_data.sleep_end - sleep_data.sleep_start).total_seconds() / 3600,
        2,
    )
    if duration_hours <= 0:
        raise ValueError("sleep_end must be after sleep_start")

    sleep_efficiency = None
    if sleep_data.time_in_bed_minutes and sleep_data.awake_time_minutes is not None:
        asleep_minutes = sleep_data.time_in_bed_minutes - sleep_data.awake_time_minutes
        if sleep_data.time_in_bed_minutes > 0:
            sleep_efficiency = round((asleep_minutes / sleep_data.time_in_bed_minutes) * 100, 2)

    return SleepRecord(
        user_id=user_id,
        sleep_start=sleep_data.sleep_start,
        sleep_end=sleep_data.sleep_end,
        duration_hours=duration_hours,
        quality_score=sleep_data.quality_score,
        deep_sleep_minutes=sleep_data.deep_sleep_minutes,
        rem_sleep_minutes=sleep_data.rem_sleep_minutes,
        awakenings_count=sleep_data.awakenings_count,
        sleep_latency_minutes=sleep_data.sleep_latency_minutes,
        awake_time_minutes=sleep_data.awake_time_minutes,
        time_in_bed_minutes=sleep_data.time_in_bed_minutes,
        sleep_efficiency=sleep_efficiency,
        day_type=sleep_data.day_type,
        notes=sleep_data.notes,
        source=sleep_data.source or "import",
    )


def _apply_batch(db: Session, user: User, batch: ImportBatchResponse, request: ImportBatchRequest) -> None:
    for h in request.hydration:
        rt = h.record_time or datetime.now()
        db.add(
            HydrationRecord(
                user_id=user.id,
                amount_ml=float(h.amount_ml),
                record_time=rt,
                source=h.source or "import",
            )
        )
        batch.hydration_created += 1

    for m in request.meals:
        db.add(
            MealRecord(
                user_id=user.id,
                meal_type=m.meal_type,
                name=m.name,
                calories=m.calories,
                protein_g=m.protein_g,
                fat_g=m.fat_g,
                carbs_g=m.carbs_g,
                fiber_g=m.fiber_g,
                sugar_g=m.sugar_g,
                caffeine_mg=m.caffeine_mg,
                water_ml=m.water_ml,
                portion_g=m.portion_g,
                glycemic_load=m.glycemic_load,
                meal_category=m.meal_category,
                minutes_before_sleep=m.minutes_before_sleep,
                is_late_meal=m.is_late_meal,
                meal_time=m.meal_time,
                notes=m.notes,
                source=m.source or "import",
            )
        )
        batch.meals_created += 1

    for s in request.sleeps:
        db.add(_sleep_record_for_user(user.id, s))
        batch.sleeps_created += 1

    for a in request.activities:
        db.add(
            ActivityRecord(
                user_id=user.id,
                activity_type=a.activity_type,
                start_time=a.start_time,
                end_time=a.end_time,
                duration_minutes=a.duration_minutes,
                steps=a.steps,
                distance_km=a.distance_km,
                calories_burned=a.calories_burned,
                avg_heart_rate=a.avg_heart_rate,
                avg_power_w=a.avg_power_w,
                avg_speed_m_s=a.avg_speed_m_s,
                intensity=a.intensity,
                activity_category=a.activity_category,
                perceived_exertion=a.perceived_exertion,
                minutes_before_sleep=a.minutes_before_sleep,
                is_evening_activity=a.is_evening_activity,
                notes=a.notes,
                source=a.source or "import",
            )
        )
        batch.activities_created += 1

    for hs in request.health_samples:
        db.add(
            HealthSample(
                user_id=user.id,
                recorded_at=hs.recorded_at,
                period_end=hs.period_end,
                metric=hs.metric,
                value1=hs.value1,
                value2=hs.value2,
                text_value=hs.text_value,
                source=hs.source or "health_connect",
            )
        )
        batch.health_samples_created += 1


def _parse_iso(value: str) -> datetime:
    v = value.strip().replace("Z", "+00:00")
    return datetime.fromisoformat(v)


def csv_text_to_batch(text: str) -> tuple[ImportBatchRequest, list[str]]:
    errors: list[str] = []
    hydration: list[HydrationCreate] = []
    meals: list[MealCreate] = []
    sleeps: list[SleepCreate] = []
    activities: list[ActivityCreate] = []

    for line_no, raw in enumerate(text.splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = [p.strip() for p in line.split(";")]
        if len(parts) < 2:
            errors.append(f"Строка {line_no}: слишком мало полей")
            continue
        kind = parts[0].lower()
        try:
            if kind == "hydration" and len(parts) >= 3:
                hydration.append(
                    HydrationCreate(
                        amount_ml=int(parts[2]),
                        record_time=_parse_iso(parts[1]),
                        source="import",
                    )
                )
            elif kind == "meal" and len(parts) >= 4:
                meals.append(
                    MealCreate(
                        meal_type=parts[1],
                        name=parts[2],
                        meal_time=_parse_iso(parts[3]),
                        calories=float(parts[4]) if len(parts) > 4 and parts[4] else None,
                        protein_g=float(parts[5]) if len(parts) > 5 and parts[5] else None,
                        fat_g=float(parts[6]) if len(parts) > 6 and parts[6] else None,
                        carbs_g=float(parts[7]) if len(parts) > 7 and parts[7] else None,
                        source="import",
                    )
                )
            elif kind == "sleep" and len(parts) >= 3:
                q = float(parts[3]) if len(parts) > 3 and parts[3] else None
                notes = parts[4] if len(parts) > 4 and parts[4] else None
                sleeps.append(
                    SleepCreate(
                        sleep_start=_parse_iso(parts[1]),
                        sleep_end=_parse_iso(parts[2]),
                        quality_score=q,
                        notes=notes,
                        source="import",
                    )
                )
            elif kind == "activity" and len(parts) >= 6:
                activities.append(
                    ActivityCreate(
                        activity_type=parts[1],
                        start_time=_parse_iso(parts[2]),
                        end_time=_parse_iso(parts[3]),
                        duration_minutes=int(parts[4]),
                        steps=int(parts[5]) if len(parts) > 5 and parts[5] else None,
                        source="import",
                    )
                )
            else:
                errors.append(f"Строка {line_no}: неизвестный тип или недостаточно полей")
        except Exception as exc:  # noqa: BLE001
            errors.append(f"Строка {line_no}: {exc}")

    return (
        ImportBatchRequest(
            hydration=hydration,
            meals=meals,
            sleeps=sleeps,
            activities=activities,
        ),
        errors,
    )


@router.post("/batch", response_model=ImportBatchResponse)
def import_batch(
    request: ImportBatchRequest,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if not any(
        [
            request.hydration,
            request.meals,
            request.sleeps,
            request.activities,
            request.health_samples,
        ]
    ):
        return ImportBatchResponse(
            errors=[
                "Нет записей для импорта: в запросе пустые списки. "
                "Если это Health Connect — за период могло не быть данных или они ещё не попали в Health Connect на телефоне."
            ],
        )

    batch = ImportBatchResponse()
    try:
        _apply_batch(db, current_user, batch, request)
        db.commit()
    except Exception as exc:  # noqa: BLE001
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Импорт не выполнен: {exc}",
        ) from exc

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "import",
        "Импортированы данные",
    )

    return batch


@router.post("/csv", response_model=ImportBatchResponse)
def import_csv(
    body: CsvImportBody,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    batch_req, parse_errors = csv_text_to_batch(body.text)
    if parse_errors and not any(
        [
            batch_req.hydration,
            batch_req.meals,
            batch_req.sleeps,
            batch_req.activities,
            batch_req.health_samples,
        ]
    ):
        return ImportBatchResponse(errors=parse_errors)

    batch = ImportBatchResponse(errors=parse_errors)
    if not any(
        [
            batch_req.hydration,
            batch_req.meals,
            batch_req.sleeps,
            batch_req.activities,
            batch_req.health_samples,
        ]
    ):
        return batch

    try:
        _apply_batch(db, current_user, batch, batch_req)
        db.commit()
    except Exception as exc:  # noqa: BLE001
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Импорт не выполнен: {exc}",
        ) from exc

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "import",
        "Импортированы данные из CSV",
    )

    return batch
