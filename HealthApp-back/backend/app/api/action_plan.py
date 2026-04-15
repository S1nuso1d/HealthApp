from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.user import User
from app.schemas.action_plan import (
    ActionPlanGenerateResponse,
    ActionPlanResponse,
    ActionPlanStatusUpdate,
)
from app.services.action_plan_builder import ActionPlanBuilder

router = APIRouter(prefix="/action-plan", tags=["Action Plan"])


@router.post(
    "/generate",
    response_model=ActionPlanGenerateResponse,
    summary="Сгенерировать action plan",
    description="Создает список action plan на основе текущих рекомендаций пользователя."
)
def generate_action_plan(
    limit: int = Query(default=5, ge=1, le=20, description="Максимальное число пунктов плана"),
    replace_existing: bool = Query(default=True, description="Заменить существующий план новым"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    items = ActionPlanBuilder.generate_for_user(
        db=db,
        user_id=current_user.id,
        limit=limit,
        replace_existing=replace_existing,
    )

    return ActionPlanGenerateResponse(
        message="Action plan успешно сгенерирован",
        created_count=len(items),
    )


@router.get(
    "/",
    response_model=list[ActionPlanResponse],
    summary="Получить action plan пользователя",
    description="Возвращает все текущие элементы action plan пользователя."
)
def get_action_plan(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return ActionPlanBuilder.get_for_user(db=db, user_id=current_user.id)


@router.get(
    "/{action_plan_id}",
    response_model=ActionPlanResponse,
    summary="Получить пункт action plan по ID",
    description="Возвращает один элемент action plan пользователя."
)
def get_action_plan_item(
    action_plan_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    item = ActionPlanBuilder.get_by_id(
        db=db,
        user_id=current_user.id,
        action_plan_id=action_plan_id,
    )

    if item is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Пункт action plan не найден",
        )

    return item


@router.patch(
    "/{action_plan_id}/status",
    response_model=ActionPlanResponse,
    summary="Изменить статус пункта action plan",
    description="Обновляет статус элемента action plan."
)
def update_action_plan_status(
    action_plan_id: int,
    payload: ActionPlanStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    item = ActionPlanBuilder.update_status(
        db=db,
        user_id=current_user.id,
        action_plan_id=action_plan_id,
        status=payload.status,
    )

    if item is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Пункт action plan не найден",
        )

    return item


@router.delete(
    "/{action_plan_id}",
    summary="Удалить пункт action plan",
    description="Удаляет элемент action plan пользователя."
)
def delete_action_plan_item(
    action_plan_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    deleted = ActionPlanBuilder.delete_item(
        db=db,
        user_id=current_user.id,
        action_plan_id=action_plan_id,
    )

    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Пункт action plan не найден",
        )

    return {"message": "Пункт action plan удален"}