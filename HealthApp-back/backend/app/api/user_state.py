from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.user import User
from app.models.user_state import UserState
from app.schemas.user_state import UserStateCreate, UserStateResponse
from app.services.analytics_sync import rebuild_user_analytics

router = APIRouter(prefix="/states", tags=["User States"])


@router.post(
    "/",
    response_model=UserStateResponse,
    summary="Добавить запись состояния",
    description="Создает новую запись субъективного состояния пользователя.",
    response_description="Созданная запись состояния"
)
def create_user_state(
    state_data: UserStateCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    new_state = UserState(
        user_id=current_user.id,
        mood=state_data.mood,
        energy=state_data.energy,
        stress=state_data.stress,
        focus=state_data.focus,
        record_time=state_data.record_time,
        notes=state_data.notes,
    )

    db.add(new_state)
    db.commit()
    db.refresh(new_state)

    rebuild_user_analytics(db, current_user.id, days=7)

    return new_state


@router.get(
    "/",
    response_model=list[UserStateResponse],
    summary="Получить все мои записи состояния",
    description="Возвращает список всех записей состояния текущего пользователя.",
    response_description="Список записей состояния"
)
def get_user_states(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    states = (
        db.query(UserState)
        .filter(UserState.user_id == current_user.id)
        .order_by(UserState.record_time.desc())
        .all()
    )
    return states


@router.get(
    "/{state_id}",
    response_model=UserStateResponse,
    summary="Получить запись состояния по ID",
    description="Возвращает одну запись состояния пользователя по её ID.",
    response_description="Запись состояния"
)
def get_user_state(
    state_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    state = (
        db.query(UserState)
        .filter(
            UserState.id == state_id,
            UserState.user_id == current_user.id
        )
        .first()
    )

    if not state:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись состояния не найдена"
        )

    return state


@router.delete(
    "/{state_id}",
    summary="Удалить запись состояния",
    description="Удаляет запись состояния пользователя по её ID.",
    response_description="Сообщение об успешном удалении"
)
def delete_user_state(
    state_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    state = (
        db.query(UserState)
        .filter(
            UserState.id == state_id,
            UserState.user_id == current_user.id
        )
        .first()
    )

    if not state:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Запись состояния не найдена"
        )

    db.delete(state)
    db.commit()

    rebuild_user_analytics(db, current_user.id, days=7)

    return {"message": "Запись состояния удалена"}