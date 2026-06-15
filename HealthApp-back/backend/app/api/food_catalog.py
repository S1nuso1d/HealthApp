"""API каталога продуктов: Open Food Facts + пользовательские дополнения."""

from fastapi import APIRouter, Depends, File, Form, HTTPException, Request, UploadFile, status
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.food_catalog import FoodCatalogItem
from app.models.user import User
from app.schemas.food_catalog import FoodCatalogItemOut, FoodCatalogSearchResponse, FoodCatalogUpsertBody
from app.services import food_catalog_service
from app.services.avatar_storage import guess_media_type, validate_avatar_bytes
from app.services.food_image_storage import find_food_image_path, save_food_image

router = APIRouter(prefix="/meal", tags=["Food catalog"])


def _request_base(request: Request) -> str:
    return str(request.base_url).rstrip("/")


@router.get("/foods/search", response_model=FoodCatalogSearchResponse, summary="Поиск продуктов")
async def search_foods(
    q: str,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    items = await food_catalog_service.search_foods(db, q, current_user.id, _request_base(request))
    return FoodCatalogSearchResponse(items=items)


@router.get("/foods/barcode/{code}", response_model=FoodCatalogItemOut, summary="Продукт по штрихкоду")
async def food_by_barcode(
    code: str,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    item = await food_catalog_service.resolve_barcode(db, code, current_user.id, _request_base(request))
    if not item:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Продукт не найден по штрихкоду")
    return item


@router.get("/foods/catalog/{item_id}", response_model=FoodCatalogItemOut, summary="Карточка продукта")
def get_catalog_item(
    item_id: int,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    row = db.query(FoodCatalogItem).filter(FoodCatalogItem.id == item_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Продукт не найден")
    return food_catalog_service.item_to_out(row, _request_base(request))


@router.get("/foods/catalog/{item_id}/image", summary="Фото продукта из каталога")
def get_catalog_image(
    item_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    path = find_food_image_path(item_id)
    if not path:
        raise HTTPException(status_code=404, detail="Фото не найдено")
    return FileResponse(path, media_type=guess_media_type(path))


@router.post("/foods/catalog", response_model=FoodCatalogItemOut, summary="Добавить или дополнить продукт")
async def create_catalog_item(
    request: Request,
    name: str = Form(...),
    barcode: str | None = Form(None),
    brand: str | None = Form(None),
    calories_100g: float | None = Form(None),
    protein_g_100g: float | None = Form(None),
    fat_g_100g: float | None = Form(None),
    carbs_g_100g: float | None = Form(None),
    off_image_url: str | None = Form(None),
    file: UploadFile | None = File(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    body = FoodCatalogUpsertBody(
        barcode=barcode,
        name=name,
        brand=brand,
        calories_100g=calories_100g,
        protein_g_100g=protein_g_100g,
        fat_g_100g=fat_g_100g,
        carbs_g_100g=carbs_g_100g,
        off_image_url=off_image_url,
    )
    existing = food_catalog_service.get_by_barcode(db, body.barcode or "") if body.barcode else None
    row = food_catalog_service.upsert_item(
        db,
        body,
        current_user.id,
        source="user_completed" if existing else "user",
        existing=existing,
    )
    if file and file.filename:
        content = await file.read()
        ok, detail = validate_avatar_bytes(content)
        if not ok:
            raise HTTPException(status_code=400, detail=detail)
        save_food_image(row.id, content, detail)
    return food_catalog_service.item_to_out(row, _request_base(request))


@router.put("/foods/catalog/{item_id}", response_model=FoodCatalogItemOut, summary="Обновить КБЖУ продукта")
def update_catalog_item(
    item_id: int,
    body: FoodCatalogUpsertBody,
    request: Request,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    row = db.query(FoodCatalogItem).filter(FoodCatalogItem.id == item_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Продукт не найден")
    updated = food_catalog_service.upsert_item(
        db,
        FoodCatalogUpsertBody(
            barcode=row.barcode,
            name=body.name or row.name,
            brand=body.brand if body.brand is not None else row.brand,
            calories_100g=body.calories_100g if body.calories_100g is not None else row.calories_100g,
            protein_g_100g=body.protein_g_100g if body.protein_g_100g is not None else row.protein_g_100g,
            fat_g_100g=body.fat_g_100g if body.fat_g_100g is not None else row.fat_g_100g,
            carbs_g_100g=body.carbs_g_100g if body.carbs_g_100g is not None else row.carbs_g_100g,
            off_image_url=row.off_image_url,
        ),
        current_user.id,
        source="user_completed",
        existing=row,
    )
    return food_catalog_service.item_to_out(updated, _request_base(request))


@router.post("/foods/catalog/{item_id}/image", response_model=FoodCatalogItemOut, summary="Загрузить фото продукта")
async def upload_catalog_image(
    item_id: int,
    request: Request,
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    row = db.query(FoodCatalogItem).filter(FoodCatalogItem.id == item_id).first()
    if not row:
        raise HTTPException(status_code=404, detail="Продукт не найден")
    content = await file.read()
    ok, detail = validate_avatar_bytes(content)
    if not ok:
        raise HTTPException(status_code=400, detail=detail)
    save_food_image(row.id, content, detail)
    return food_catalog_service.item_to_out(row, _request_base(request))
