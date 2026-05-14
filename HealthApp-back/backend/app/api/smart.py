from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.smart_reminder import SmartReminder
from app.models.smart_trigger import SmartTrigger
from app.models.user import User
from app.schemas.smart_trigger import (
    SmartReminderResponse,
    SmartReminderStatusResponse,
    SmartTriggerGenerateResponse,
    SmartTriggerResponse,
)
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/smart", tags=["Smart Triggers"])


@router.post(
    "/generate",
    response_model=SmartTriggerGenerateResponse,
    summary="Сгенерировать умные триггеры и reminders",
)
def generate_smart_items(
    period_days: int = Query(default=7, ge=3, le=30),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return generate_smart_triggers_and_reminders(
        db=db,
        user_id=current_user.id,
        period_days=period_days,
    )


@router.get(
    "/triggers/active",
    response_model=list[SmartTriggerResponse],
    summary="Получить активные триггеры",
)
def get_active_triggers(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return (
        db.query(SmartTrigger)
        .filter(
            SmartTrigger.user_id == current_user.id,
            SmartTrigger.is_active == True,
            SmartTrigger.is_resolved == False,
        )
        .order_by(SmartTrigger.created_at.desc())
        .all()
    )


@router.get(
    "/reminders/active",
    response_model=list[SmartReminderResponse],
    summary="Получить активные reminders",
)
def get_active_reminders(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return (
        db.query(SmartReminder)
        .filter(
            SmartReminder.user_id == current_user.id,
            SmartReminder.is_active == True,
        )
        .order_by(SmartReminder.created_at.desc())
        .all()
    )


@router.patch(
    "/reminders/{reminder_id}/read",
    response_model=SmartReminderStatusResponse,
    summary="Отметить reminder как прочитанный",
)
def mark_reminder_read(
    reminder_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    reminder = (
        db.query(SmartReminder)
        .filter(
            SmartReminder.id == reminder_id,
            SmartReminder.user_id == current_user.id,
        )
        .first()
    )

    if not reminder:
        raise HTTPException(status_code=404, detail="Reminder не найден")

    if reminder.status == "new":
        reminder.status = "read"

    db.commit()

    return {"message": "Reminder отмечен как прочитанный"}


@router.patch(
    "/reminders/{reminder_id}/complete",
    response_model=SmartReminderStatusResponse,
    summary="Отметить reminder как выполненный",
)
def complete_reminder(
    reminder_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    reminder = (
        db.query(SmartReminder)
        .filter(
            SmartReminder.id == reminder_id,
            SmartReminder.user_id == current_user.id,
        )
        .first()
    )

    if not reminder:
        raise HTTPException(status_code=404, detail="Reminder не найден")

    reminder.status = "completed"
    reminder.is_active = False

    if reminder.trigger_id:
        trigger = (
            db.query(SmartTrigger)
            .filter(SmartTrigger.id == reminder.trigger_id)
            .first()
        )
        if trigger:
            trigger.is_resolved = True
            trigger.is_active = False

    db.commit()

    return {"message": "Reminder отмечен как выполненный"}


@router.patch(
    "/reminders/{reminder_id}/dismiss",
    response_model=SmartReminderStatusResponse,
    summary="Отклонить reminder",
)
def dismiss_reminder(
    reminder_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    reminder = (
        db.query(SmartReminder)
        .filter(
            SmartReminder.id == reminder_id,
            SmartReminder.user_id == current_user.id,
        )
        .first()
    )

    if not reminder:
        raise HTTPException(status_code=404, detail="Reminder не найден")

    reminder.status = "dismissed"
    reminder.is_active = False

    db.commit()

    return {"message": "Reminder отклонен"}