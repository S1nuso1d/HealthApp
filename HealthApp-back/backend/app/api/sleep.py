from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.sleep import SleepRecord
from app.models.user import User
from app.schemas.sleep import SleepCreate, SleepResponse
from app.services.analytics_sync import rebuild_user_analytics
from app.services.realtime_manager import realtime_manager
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/sleep", tags=["Sleep"])


@router.post(
    "/",
    response_model=SleepResponse,
    summary="Добавить запись сна",
    description="Создает новую запись сна пользователя.",
    response_description="Созданная запись сна",
)
def create_sleep_record(
    sleep_data: SleepCreate,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    duration_hours = round(
        (sleep_data.sleep_end - sleep_data.sleep_start).total_seconds() / 3600,
        2
    )

    if duration_hours <= 0:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Время окончания сна должно быть позже времени начала сна"
        )

    sleep_efficiency = None
    if sleep_data.time_in_bed_minutes and sleep_data.awake_time_minutes is not None:
        asleep_minutes = sleep_data.time_in_bed_minutes - sleep_data.awake_time_minutes
        if sleep_data.time_in_bed_minutes > 0:
            sleep_efficiency = round((asleep_minutes / sleep_data.time_in_bed_minutes) * 100, 2)

    new_record = SleepRecord(
        user_id=current_user.id,
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
        source=sleep_data.source or "manual",
    )

    db.add(new_record)
    db.commit()
    db.refresh(new_record)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "sleep",
        "Обновлены данные сна",
    )

    return new_record


@router.get(
    "/history",
    response_model=list[SleepResponse],
    summary="Получить историю сна",
    description="Возвращает все записи сна текущего пользователя.",
    response_description="Список записей сна",
)
def get_sleep_history(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    records = (
        db.query(SleepRecord)
        .filter(SleepRecord.user_id == current_user.id)
        .order_by(SleepRecord.sleep_start.desc())
        .all()
    )
    return records


@router.get(
    "/today",
    response_model=list[SleepResponse],
    summary="Получить записи сна за сегодня",
    description="Возвращает записи сна текущего пользователя за текущий день.",
    response_description="Список записей сна за сегодня",
)
def get_today_sleep(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from datetime import datetime, time

    today = datetime.now().date()
    day_start = datetime.combine(today, time.min)
    day_end = datetime.combine(today, time.max)

    records = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.user_id == current_user.id,
            SleepRecord.sleep_start >= day_start,
            SleepRecord.sleep_start <= day_end,
        )
        .order_by(SleepRecord.sleep_start.desc())
        .all()
    )
    return records


@router.get(
    "/{sleep_id}",
    response_model=SleepResponse,
    summary="Получить запись сна по ID",
    description="Возвращает одну запись сна текущего пользователя по ее ID.",
    response_description="Одна запись сна",
)
def get_sleep_record_by_id(
    sleep_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.id == sleep_id,
            SleepRecord.user_id == current_user.id,
        )
        .first()
    )

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись сна не найдена"
        )

    return record


@router.delete(
    "/{sleep_id}",
    summary="Удалить запись сна",
    description="Удаляет запись сна текущего пользователя по ID.",
    response_description="Результат удаления",
)
def delete_sleep_record(
    sleep_id: int,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(SleepRecord)
        .filter(
            SleepRecord.id == sleep_id,
            SleepRecord.user_id == current_user.id,
        )
        .first()
    )

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись сна не найдена"
        )

    db.delete(record)
    db.commit()

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "sleep",
        "Удалена запись сна",
    )

    return {"message": "Запись сна успешно удалена"}