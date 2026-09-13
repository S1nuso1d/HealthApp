from __future__ import annotations

from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.models.food_catalog import FoodCatalogItem
from app.schemas.food_catalog import FoodCatalogItemOut, FoodCatalogUpsertBody
from app.services import openfoodfacts_client
from app.services.food_image_storage import find_food_image_path


def _has_full_macros(
    calories: float | None,
    protein: float | None,
    fat: float | None,
    carbs: float | None,
) -> bool:
    return all(v is not None for v in (calories, protein, fat, carbs))


def _image_url_for(item: FoodCatalogItem, request_base: str | None = None) -> str | None:
    if find_food_image_path(item.id):
        base = (request_base or "").rstrip("/")
        return f"{base}/meal/foods/catalog/{item.id}/image" if base else f"/meal/foods/catalog/{item.id}/image"
    return item.off_image_url


def item_to_out(item: FoodCatalogItem, request_base: str | None = None) -> FoodCatalogItemOut:
    complete = bool(item.is_complete) or _has_full_macros(
        item.calories_100g,
        item.protein_g_100g,
        item.fat_g_100g,
        item.carbs_g_100g,
    )
    return FoodCatalogItemOut(
        id=item.id,
        barcode=item.barcode,
        name=item.name,
        brand=item.brand,
        calories_100g=item.calories_100g,
        protein_g_100g=item.protein_g_100g,
        fat_g_100g=item.fat_g_100g,
        carbs_g_100g=item.carbs_g_100g,
        image_url=_image_url_for(item, request_base),
        source=item.source,
        is_complete=complete,
        needs_completion=not complete,
    )


def dict_to_out(data: dict, request_base: str | None = None) -> FoodCatalogItemOut:
    complete = bool(data.get("is_complete"))
    return FoodCatalogItemOut(
        id=data.get("id"),
        barcode=data.get("barcode"),
        name=data["name"],
        brand=data.get("brand"),
        calories_100g=data.get("calories_100g"),
        protein_g_100g=data.get("protein_g_100g"),
        fat_g_100g=data.get("fat_g_100g"),
        carbs_g_100g=data.get("carbs_g_100g"),
        image_url=data.get("off_image_url"),
        source=data.get("source", "openfoodfacts"),
        is_complete=complete,
        needs_completion=not complete,
    )


def get_by_barcode(db: Session, barcode: str) -> FoodCatalogItem | None:
    code = barcode.strip()
    if not code:
        return None
    return db.query(FoodCatalogItem).filter(FoodCatalogItem.barcode == code).first()


def get_by_barcodes(db: Session, barcodes: list[str]) -> dict[str, FoodCatalogItem]:
    """Один запрос вместо одного на каждый штрихкод из результатов поиска."""
    codes = [c.strip() for c in barcodes if c and c.strip()]
    if not codes:
        return {}
    rows = (
        db.query(FoodCatalogItem)
        .filter(FoodCatalogItem.barcode.in_(codes))
        .all()
    )
    return {row.barcode: row for row in rows if row.barcode}


def search_local(db: Session, query: str, limit: int = 12) -> list[FoodCatalogItem]:
    q = f"%{query.strip().lower()}%"
    return (
        db.query(FoodCatalogItem)
        .filter(
            or_(
                FoodCatalogItem.name.ilike(q),
                FoodCatalogItem.brand.ilike(q),
                FoodCatalogItem.barcode.ilike(q),
            )
        )
        .order_by(FoodCatalogItem.is_complete.desc(), FoodCatalogItem.updated_at.desc())
        .limit(limit)
        .all()
    )


def upsert_item(
    db: Session,
    body: FoodCatalogUpsertBody,
    user_id: int,
    *,
    source: str = "user",
    existing: FoodCatalogItem | None = None,
    lookup_existing: bool = True,
    commit: bool = True,
) -> FoodCatalogItem:
    barcode = body.barcode.strip() if body.barcode else None
    row = existing
    if row is None and barcode and lookup_existing:
        row = get_by_barcode(db, barcode)

    if row is None:
        row = FoodCatalogItem(
            barcode=barcode,
            name=(body.name or "").strip() or "Продукт",
            brand=body.brand,
            calories_100g=body.calories_100g,
            protein_g_100g=body.protein_g_100g,
            fat_g_100g=body.fat_g_100g,
            carbs_g_100g=body.carbs_g_100g,
            off_image_url=body.off_image_url,
            source=source,
            created_by_user_id=user_id,
        )
        db.add(row)
    else:
        if body.name:
            row.name = body.name.strip() or row.name
        if body.brand is not None:
            row.brand = body.brand
        if body.calories_100g is not None:
            row.calories_100g = body.calories_100g
        if body.protein_g_100g is not None:
            row.protein_g_100g = body.protein_g_100g
        if body.fat_g_100g is not None:
            row.fat_g_100g = body.fat_g_100g
        if body.carbs_g_100g is not None:
            row.carbs_g_100g = body.carbs_g_100g
        if body.off_image_url and not row.off_image_url:
            row.off_image_url = body.off_image_url
        if source == "user_completed":
            row.source = "user_completed"

    row.is_complete = _has_full_macros(
        row.calories_100g,
        row.protein_g_100g,
        row.fat_g_100g,
        row.carbs_g_100g,
    )
    if commit:
        db.commit()
        db.refresh(row)
    else:
        # flush проставляет row.id, нужный для ссылки на картинку, но не завершает
        # транзакцию — вызывающий код коммитит один раз для всей пачки
        db.flush()
    return row


def merge_off_into_db(
    db: Session,
    off_data: dict,
    user_id: int,
    *,
    existing: FoodCatalogItem | None = None,
    lookup_existing: bool = True,
    commit: bool = True,
) -> FoodCatalogItem:
    body = FoodCatalogUpsertBody(
        barcode=off_data.get("barcode"),
        name=off_data["name"],
        brand=off_data.get("brand"),
        calories_100g=off_data.get("calories_100g"),
        protein_g_100g=off_data.get("protein_g_100g"),
        fat_g_100g=off_data.get("fat_g_100g"),
        carbs_g_100g=off_data.get("carbs_g_100g"),
        off_image_url=off_data.get("off_image_url"),
    )
    if existing is None and lookup_existing:
        existing = get_by_barcode(db, body.barcode or "")
    source = "openfoodfacts" if off_data.get("source") == "openfoodfacts" else "user"
    if existing and existing.is_complete:
        return existing
    if existing and not existing.is_complete:
        merged = FoodCatalogUpsertBody(
            barcode=body.barcode,
            name=existing.name or body.name,
            brand=existing.brand or body.brand,
            calories_100g=existing.calories_100g if existing.calories_100g is not None else body.calories_100g,
            protein_g_100g=existing.protein_g_100g if existing.protein_g_100g is not None else body.protein_g_100g,
            fat_g_100g=existing.fat_g_100g if existing.fat_g_100g is not None else body.fat_g_100g,
            carbs_g_100g=existing.carbs_g_100g if existing.carbs_g_100g is not None else body.carbs_g_100g,
            off_image_url=existing.off_image_url or body.off_image_url,
        )
        return upsert_item(
            db,
            merged,
            user_id,
            source=existing.source,
            existing=existing,
            lookup_existing=False,
            commit=commit,
        )
    return upsert_item(
        db,
        body,
        user_id,
        source=source,
        existing=existing,
        lookup_existing=lookup_existing,
        commit=commit,
    )


async def resolve_barcode(db: Session, code: str, user_id: int, request_base: str) -> FoodCatalogItemOut | None:
    local = get_by_barcode(db, code)
    if local and local.is_complete:
        return item_to_out(local, request_base)

    off = await openfoodfacts_client.fetch_product_by_barcode(code)
    if off:
        row = merge_off_into_db(db, off, user_id)
        return item_to_out(row, request_base)

    if local:
        return item_to_out(local, request_base)
    return None


async def search_foods(db: Session, query: str, user_id: int, request_base: str) -> list[FoodCatalogItemOut]:
    local_rows = search_local(db, query)
    items: list[FoodCatalogItemOut] = [item_to_out(r, request_base) for r in local_rows]
    seen_barcodes = {i.barcode for i in items if i.barcode}
    seen_names = {i.name.lower() for i in items}

    off_hits = await openfoodfacts_client.search_products(query)
    # Раньше здесь было по SELECT и COMMIT на каждый результат Open Food Facts
    existing_by_barcode = get_by_barcodes(
        db, [str(off.get("barcode")) for off in off_hits if off.get("barcode")]
    )
    touched_db = False
    for off in off_hits:
        name_key = off["name"].lower()
        barcode = off.get("barcode")
        if barcode and barcode in seen_barcodes:
            continue
        if name_key in seen_names:
            continue
        if barcode:
            row = merge_off_into_db(
                db,
                off,
                user_id,
                existing=existing_by_barcode.get(str(barcode)),
                lookup_existing=False,
                commit=False,
            )
            touched_db = True
            items.append(item_to_out(row, request_base))
        else:
            items.append(dict_to_out(off, request_base))
        if barcode:
            seen_barcodes.add(barcode)
        seen_names.add(name_key)
        if len(items) >= 20:
            break
    if touched_db:
        db.commit()
    return items[:20]
