from datetime import datetime, time

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.user import User
from app.schemas.hydration import (
    HydrationCreate,
    HydrationResponse,
    HydrationSummaryResponse,
    HydrationUpdate,
)
from app.services.analytics_sync import rebuild_user_analytics
from app.services.realtime_manager import realtime_manager
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

try:
    from app.models.hydration import HydrationRecord
except Exception as exc:
    raise RuntimeError(
        "Не найден app.models.hydration.HydrationRecord. "
        "Проверь модель гидратации."
    ) from exc

router = APIRouter(prefix="/hydration", tags=["Hydration"])


@router.post(
    "/",
    response_model=HydrationResponse,
    summary="Добавить запись о воде",
    description="Создает новую запись потребления воды. Если время не передано, ставится текущее.",
    response_description="Созданная запись воды",
)
def create_hydration_record(
    hydration_data: HydrationCreate,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record_time = hydration_data.record_time or datetime.now()

    new_record = HydrationRecord(
        user_id=current_user.id,
        amount_ml=hydration_data.amount_ml,
        record_time=record_time,
        source=hydration_data.source,
    )

    db.add(new_record)
    db.commit()
    db.refresh(new_record)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "hydration",
        "Обновлены данные гидратации",
    )

    return new_record


@router.get(
    "/today",
    response_model=HydrationSummaryResponse,
    summary="Получить сводку по воде за сегодня",
    description="Возвращает суммарное количество воды за сегодня и записи за день.",
    response_description="Сводка по гидратации за сегодня",
)
def get_today_hydration_summary(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    today = datetime.now().date()
    day_start = datetime.combine(today, time.min)
    day_end = datetime.combine(today, time.max)

    records = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.user_id == current_user.id,
            HydrationRecord.record_time >= day_start,
            HydrationRecord.record_time <= day_end,
        )
        .order_by(HydrationRecord.record_time.desc())
        .all()
    )

    total_ml = sum((record.amount_ml or 0) for record in records)

    return HydrationSummaryResponse(
        total_ml=total_ml,
        records=records,
    )


@router.get(
    "/history",
    response_model=list[HydrationResponse],
    summary="Получить историю воды",
    description="Возвращает все записи воды текущего пользователя.",
    response_description="История записей гидратации",
)
def get_hydration_history(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    records = (
        db.query(HydrationRecord)
        .filter(HydrationRecord.user_id == current_user.id)
        .order_by(HydrationRecord.record_time.desc())
        .all()
    )
    return records


@router.get(
    "/{record_id}",
    response_model=HydrationResponse,
    summary="Получить запись воды по ID",
    description="Возвращает одну запись воды по ID.",
    response_description="Одна запись гидратации",
)
def get_hydration_record_by_id(
    record_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.id == record_id,
            HydrationRecord.user_id == current_user.id,
        )
        .first()
    )

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись гидратации не найдена"
        )

    return record


@router.put(
    "/{record_id}",
    response_model=HydrationResponse,
    summary="Обновить запись воды",
    description="Изменяет объём и при необходимости время записи.",
)
def update_hydration_record(
    record_id: int,
    data: HydrationUpdate,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.id == record_id,
            HydrationRecord.user_id == current_user.id,
        )
        .first()
    )

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись гидратации не найдена"
        )

    record.amount_ml = float(data.amount_ml)
    if data.record_time is not None:
        record.record_time = data.record_time
    if data.source is not None:
        record.source = data.source

    db.add(record)
    db.commit()
    db.refresh(record)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "hydration",
        "Обновлена запись гидратации",
    )

    return record


@router.delete(
    "/{record_id}",
    summary="Удалить запись воды",
    description="Удаляет запись гидратации текущего пользователя.",
    response_description="Результат удаления",
)
def delete_hydration_record(
    record_id: int,
    background_tasks: BackgroundTasks,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    record = (
        db.query(HydrationRecord)
        .filter(
            HydrationRecord.id == record_id,
            HydrationRecord.user_id == current_user.id,
        )
        .first()
    )

    if not record:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись гидратации не найдена"
        )

    db.delete(record)
    db.commit()

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "hydration",
        "Удалена запись гидратации",
    )

    return {"message": "Запись гидратации успешно удалена"}