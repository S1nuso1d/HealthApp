from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import settings
from app.db.database import get_db
from app.models.integration_credential import IntegrationCredential
from app.models.user import User
from app.schemas.integrations import FatSecretLinkBody, FatSecretSearchResponse
from app.services.fatsecret_client import food_get_by_barcode, food_get_by_id, foods_search

router = APIRouter(prefix="/integrations", tags=["Integrations"])


def _fatsecret_creds_row(db: Session, user_id: int) -> IntegrationCredential | None:
    return (
        db.query(IntegrationCredential)
        .filter(
            IntegrationCredential.user_id == user_id,
            IntegrationCredential.provider == "fatsecret",
        )
        .first()
    )


@router.post(
    "/fatsecret/link",
    summary="Сохранить OAuth-токены FatSecret",
    description="Токены выдаются после авторизации приложения в кабинете FatSecret Platform.",
)
def link_fatsecret(
    body: FatSecretLinkBody,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    row = _fatsecret_creds_row(db, current_user.id)
    if row:
        row.access_token = body.access_token
        row.access_secret = body.access_secret
    else:
        row = IntegrationCredential(
            user_id=current_user.id,
            provider="fatsecret",
            access_token=body.access_token,
            access_secret=body.access_secret,
        )
        db.add(row)
    db.commit()
    return {"message": "FatSecret подключён"}


@router.delete(
    "/fatsecret/link",
    summary="Отвязать FatSecret",
)
def unlink_fatsecret(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    db.query(IntegrationCredential).filter(
        IntegrationCredential.user_id == current_user.id,
        IntegrationCredential.provider == "fatsecret",
    ).delete()
    db.commit()
    return {"message": "FatSecret отвязан"}


@router.get(
    "/fatsecret/foods/search",
    response_model=FatSecretSearchResponse,
    summary="Поиск продуктов FatSecret",
)
def fatsecret_food_search(
    q: str,
    current_user: User = Depends(get_current_user),
):
    if not settings.FATSECRET_CONSUMER_KEY or not settings.FATSECRET_CONSUMER_SECRET:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="FatSecret не настроен: задайте FATSECRET_CONSUMER_KEY и FATSECRET_CONSUMER_SECRET на сервере.",
        )
    if not q.strip():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Пустой запрос")

    try:
        # foods.search — публичный метод, достаточно consumer key/secret (см. authentication guide).
        raw = foods_search(
            settings.FATSECRET_CONSUMER_KEY,
            settings.FATSECRET_CONSUMER_SECRET,
            q,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Ошибка FatSecret: {exc}",
        ) from exc

    return FatSecretSearchResponse(raw=raw)


@router.get(
    "/fatsecret/food",
    response_model=FatSecretSearchResponse,
    summary="Детали продукта FatSecret по food_id",
)
def fatsecret_food_get(
    food_id: str,
    current_user: User = Depends(get_current_user),
):
    if not settings.FATSECRET_CONSUMER_KEY or not settings.FATSECRET_CONSUMER_SECRET:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="FatSecret не настроен на сервере.",
        )
    if not food_id.strip():
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Пустой food_id")
    try:
        raw = food_get_by_id(
            settings.FATSECRET_CONSUMER_KEY,
            settings.FATSECRET_CONSUMER_SECRET,
            food_id,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Ошибка FatSecret: {exc}",
        ) from exc

    return FatSecretSearchResponse(raw=raw)


@router.get(
    "/fatsecret/foods/barcode",
    response_model=FatSecretSearchResponse,
    summary="Поиск продукта по штрихкоду",
)
def fatsecret_barcode(
    barcode: str,
    current_user: User = Depends(get_current_user),
):
    if not settings.FATSECRET_CONSUMER_KEY or not settings.FATSECRET_CONSUMER_SECRET:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="FatSecret не настроен на сервере.",
        )
    try:
        raw = food_get_by_barcode(
            settings.FATSECRET_CONSUMER_KEY,
            settings.FATSECRET_CONSUMER_SECRET,
            barcode,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Ошибка FatSecret: {exc}",
        ) from exc

    return FatSecretSearchResponse(raw=raw)
