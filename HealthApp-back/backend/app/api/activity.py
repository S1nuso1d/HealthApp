from datetime import datetime, time

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.activity import ActivityRecord
from app.models.user import User
from app.schemas.activity import ActivityCreate, ActivityOut
from app.services.analytics_sync import rebuild_user_analytics
from app.services.realtime_manager import realtime_manager
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/activity", tags=["Activity"])


@router.post("/", response_model=ActivityOut)
def create_activity(
    data: ActivityCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    activity = ActivityRecord(
        user_id=current_user.id,
        activity_type=data.activity_type,
        start_time=data.start_time,
        end_time=data.end_time,
        duration_minutes=data.duration_minutes,
        steps=data.steps,
        distance_km=data.distance_km,
        calories_burned=data.calories_burned,
        avg_heart_rate=data.avg_heart_rate,
        avg_power_w=data.avg_power_w,
        avg_speed_m_s=data.avg_speed_m_s,
        intensity=data.intensity,
        activity_category=data.activity_category,
        perceived_exertion=data.perceived_exertion,
        minutes_before_sleep=data.minutes_before_sleep,
        is_evening_activity=data.is_evening_activity,
        notes=data.notes,
        source=data.source,
    )
    db.add(activity)
    db.commit()
    db.refresh(activity)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "activity",
        "Обновлены данные активности",
    )

    return activity


@router.get("/today", response_model=ActivityOut | None)
def get_today_activity(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    today = datetime.now().date()
    day_start = datetime.combine(today, time.min)
    day_end = datetime.combine(today, time.max)

    activity = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.user_id == current_user.id,
            ActivityRecord.start_time >= day_start,
            ActivityRecord.start_time <= day_end,
        )
        .order_by(ActivityRecord.start_time.desc())
        .first()
    )

    return activity


@router.get("/history", response_model=list[ActivityOut])
def get_activity_history(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    activities = (
        db.query(ActivityRecord)
        .filter(ActivityRecord.user_id == current_user.id)
        .order_by(ActivityRecord.start_time.desc())
        .all()
    )
    return activities


@router.get("/{activity_id}", response_model=ActivityOut)
def get_activity_by_id(
    activity_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    activity = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.id == activity_id,
            ActivityRecord.user_id == current_user.id,
        )
        .first()
    )
    if not activity:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись активности не найдена")
    return activity


@router.put("/{activity_id}", response_model=ActivityOut)
def update_activity(
    activity_id: int,
    data: ActivityCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    activity = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.id == activity_id,
            ActivityRecord.user_id == current_user.id,
        )
        .first()
    )
    if not activity:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись активности не найдена")

    activity.activity_type = data.activity_type
    activity.start_time = data.start_time
    activity.end_time = data.end_time
    activity.duration_minutes = data.duration_minutes
    activity.steps = data.steps
    activity.distance_km = data.distance_km
    activity.calories_burned = data.calories_burned
    activity.avg_heart_rate = data.avg_heart_rate
    activity.avg_power_w = data.avg_power_w
    activity.avg_speed_m_s = data.avg_speed_m_s
    activity.intensity = data.intensity
    activity.activity_category = data.activity_category
    activity.perceived_exertion = data.perceived_exertion
    activity.minutes_before_sleep = data.minutes_before_sleep
    activity.is_evening_activity = data.is_evening_activity
    activity.notes = data.notes
    activity.source = data.source

    db.add(activity)
    db.commit()
    db.refresh(activity)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "activity",
        "Обновлены данные активности",
    )

    return activity


@router.delete("/{activity_id}")
def delete_activity(
    activity_id: int,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    activity = (
        db.query(ActivityRecord)
        .filter(
            ActivityRecord.id == activity_id,
            ActivityRecord.user_id == current_user.id,
        )
        .first()
    )
    if not activity:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись активности не найдена")

    db.delete(activity)
    db.commit()

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "activity",
        "Удалена запись активности",
    )

    return {"message": "Запись активности удалена"}