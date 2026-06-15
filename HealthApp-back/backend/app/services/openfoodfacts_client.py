"""Клиент Open Food Facts (российский контур + world fallback)."""

from __future__ import annotations

import logging
from typing import Any

import httpx

logger = logging.getLogger(__name__)

USER_AGENT = "HealthApp/1.0 (nutrition; contact@healthapp.local)"
OFF_RU_PRODUCT_URL = "https://ru.openfoodfacts.org/api/v2/product/{code}"
OFF_WORLD_PRODUCT_URL = "https://world.openfoodfacts.org/api/v2/product/{code}"
OFF_RU_SEARCH_URL = "https://ru.openfoodfacts.org/cgi/search.pl"
OFF_FIELDS = "product_name,product_name_ru,brands,nutriments,image_front_url,code,countries_tags"


def _nutriment(nutriments: dict[str, Any], *keys: str) -> float | None:
    for key in keys:
        val = nutriments.get(key)
        if val is not None:
            try:
                return float(val)
            except (TypeError, ValueError):
                continue
    return None


def _normalize_product(product: dict[str, Any], code: str) -> dict[str, Any]:
    nutriments = product.get("nutriments") or {}
    name = (
        product.get("product_name_ru")
        or product.get("product_name")
        or product.get("product_name_en")
        or "Неизвестный продукт"
    )
    calories = _nutriment(
        nutriments,
        "energy-kcal_100g",
        "energy_kcal_100g",
        "energy-kcal",
    )
    protein = _nutriment(nutriments, "proteins_100g", "proteins")
    fat = _nutriment(nutriments, "fat_100g", "fat")
    carbs = _nutriment(nutriments, "carbohydrates_100g", "carbohydrates")
    has_macros = any(v is not None and v > 0 for v in (protein, fat, carbs))
    is_complete = calories is not None and has_macros
    return {
        "barcode": code,
        "name": str(name).strip(),
        "brand": (product.get("brands") or "").strip() or None,
        "calories_100g": calories,
        "protein_g_100g": protein,
        "fat_g_100g": fat,
        "carbs_g_100g": carbs,
        "off_image_url": product.get("image_front_url"),
        "source": "openfoodfacts",
        "is_complete": is_complete,
    }


async def fetch_product_by_barcode(code: str) -> dict[str, Any] | None:
    barcode = code.strip()
    if not barcode:
        return None
    headers = {"User-Agent": USER_AGENT}
    params = {"fields": OFF_FIELDS, "lc": "ru"}
    async with httpx.AsyncClient(timeout=12.0, headers=headers) as client:
        for url_tpl in (OFF_RU_PRODUCT_URL, OFF_WORLD_PRODUCT_URL):
            try:
                resp = await client.get(url_tpl.format(code=barcode), params=params)
                resp.raise_for_status()
                data = resp.json()
                if data.get("status") == 1 and data.get("product"):
                    return _normalize_product(data["product"], barcode)
            except Exception as exc:
                logger.warning("OFF barcode %s via %s: %s", barcode, url_tpl, exc)
    return None


async def search_products(query: str, page_size: int = 12) -> list[dict[str, Any]]:
    q = query.strip()
    if len(q) < 2:
        return []
    headers = {"User-Agent": USER_AGENT}
    params = {
        "search_terms": q,
        "search_simple": 1,
        "action": "process",
        "json": 1,
        "page_size": page_size,
        "lc": "ru",
        "fields": OFF_FIELDS,
    }
    async with httpx.AsyncClient(timeout=12.0, headers=headers) as client:
        try:
            resp = await client.get(OFF_RU_SEARCH_URL, params=params)
            resp.raise_for_status()
            data = resp.json()
        except Exception as exc:
            logger.warning("OFF search %r: %s", q, exc)
            return []
    products = data.get("products") or []
    out: list[dict[str, Any]] = []
    seen: set[str] = set()
    for product in products:
        code = str(product.get("code") or "").strip()
        if not code or code in seen:
            continue
        seen.add(code)
        out.append(_normalize_product(product, code))
    return out
