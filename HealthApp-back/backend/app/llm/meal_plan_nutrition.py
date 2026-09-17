"""Пересчёт КБЖУ плана питания по продуктам из каталога / справочника."""

from __future__ import annotations

import logging
import re

from sqlalchemy.orm import Session

from app.models.food_catalog import FoodCatalogItem

logger = logging.getLogger(__name__)

# Типичные КБЖУ на 100 г (домашние продукты). Используем, если в каталоге нет совпадения.
MACROS_PER_100G: dict[str, tuple[float, float, float, float]] = {
    "овсянка": (350, 12.0, 6.0, 60.0),
    "овсян": (350, 12.0, 6.0, 60.0),
    "гречка": (330, 12.5, 3.5, 65.0),
    "рис": (340, 7.0, 1.0, 75.0),
    "макарон": (350, 12.0, 1.5, 72.0),
    "паста": (350, 12.0, 1.5, 72.0),
    "лапша": (340, 11.0, 1.5, 70.0),
    "манка": (330, 10.0, 1.0, 70.0),
    "хлеб": (250, 8.0, 3.0, 48.0),
    "курица": (165, 31.0, 3.5, 0.0),
    "куриц": (165, 31.0, 3.5, 0.0),
    "индейка": (140, 29.0, 2.0, 0.0),
    "говядина": (200, 26.0, 10.0, 0.0),
    "свинина": (240, 22.0, 16.0, 0.0),
    "фарш": (220, 20.0, 15.0, 0.0),
    "котлет": (220, 18.0, 14.0, 5.0),
    "рыба": (110, 20.0, 3.0, 0.0),
    "лосось": (180, 20.0, 10.0, 0.0),
    "минтай": (75, 16.0, 1.0, 0.0),
    "треска": (80, 18.0, 0.7, 0.0),
    "творог": (120, 16.0, 5.0, 3.0),
    "сыр": (350, 25.0, 27.0, 0.0),
    "молоко": (55, 3.0, 2.5, 5.0),
    "кефир": (50, 3.0, 2.0, 4.0),
    "сметана": (200, 2.5, 20.0, 3.0),
    "йогурт": (70, 4.0, 2.0, 8.0),
    "яйц": (155, 13.0, 11.0, 1.0),
    "омлет": (155, 13.0, 11.0, 1.0),
    "яичниц": (180, 12.0, 14.0, 1.0),
    "картоф": (80, 2.0, 0.2, 17.0),
    "морков": (40, 1.0, 0.2, 9.0),
    "капуст": (30, 1.5, 0.2, 5.0),
    "помидор": (20, 1.0, 0.2, 4.0),
    "огурец": (15, 0.7, 0.1, 3.0),
    "овощ": (35, 1.5, 0.2, 7.0),
    "салат": (25, 1.5, 0.3, 4.0),
    "борщ": (55, 3.0, 2.0, 6.0),
    "суп": (50, 3.0, 1.5, 5.0),
    "банан": (90, 1.2, 0.3, 22.0),
    "яблок": (50, 0.4, 0.2, 12.0),
    "ягод": (40, 0.8, 0.3, 8.0),
    "масло": (750, 0.5, 82.0, 0.5),
    "тофу": (120, 12.0, 7.0, 2.0),
    "чечевиц": (120, 9.0, 0.5, 20.0),
    "нут": (160, 8.0, 2.5, 27.0),
    "фасол": (120, 8.0, 0.5, 20.0),
}


def _normalize(text: str) -> str:
    return re.sub(r"\s+", " ", (text or "").strip().lower())


def _macros_from_builtin(name: str) -> tuple[float, float, float, float] | None:
    lower = _normalize(name)
    for key, macros in MACROS_PER_100G.items():
        if key in lower:
            return macros
    return None


def _macros_from_catalog(db: Session, name: str) -> tuple[float, float, float, float] | None:
    q = _normalize(name)
    if not q:
        return None
    # Точное / частичное совпадение по имени в локальном каталоге (FatSecret/OFF уже сохранены туда)
    row = (
        db.query(FoodCatalogItem)
        .filter(FoodCatalogItem.name.ilike(f"%{q}%"))
        .filter(FoodCatalogItem.calories_100g.isnot(None))
        .order_by(FoodCatalogItem.is_complete.desc(), FoodCatalogItem.updated_at.desc())
        .first()
    )
    if row is None and " " in q:
        # Пробуем первое слово (например «Курица» из «Курица филе»)
        first = q.split()[0]
        row = (
            db.query(FoodCatalogItem)
            .filter(FoodCatalogItem.name.ilike(f"%{first}%"))
            .filter(FoodCatalogItem.calories_100g.isnot(None))
            .order_by(FoodCatalogItem.is_complete.desc(), FoodCatalogItem.updated_at.desc())
            .first()
        )
    if row is None:
        return None
    return (
        float(row.calories_100g or 0),
        float(row.protein_g_100g or 0),
        float(row.fat_g_100g or 0),
        float(row.carbs_g_100g or 0),
    )


def resolve_macros_per_100g(db: Session, name: str) -> tuple[float, float, float, float]:
    catalog = _macros_from_catalog(db, name)
    if catalog is not None:
        return catalog
    builtin = _macros_from_builtin(name)
    if builtin is not None:
        return builtin
    # Нейтральный fallback — лучше, чем выдуманные LLM цифры
    return (120.0, 8.0, 4.0, 12.0)


def _scale(macros_100g: tuple[float, float, float, float], grams: float) -> tuple[float, float, float, float]:
    factor = max(grams, 0.0) / 100.0
    return tuple(round(v * factor, 1) for v in macros_100g)  # type: ignore[return-value]


def apply_catalog_nutrition_to_days(db: Session, days: list[dict]) -> list[dict]:
    """Пересчитывает КБЖУ блюд и дней по ингредиентам × справочник/каталог."""
    out: list[dict] = []
    for day in days:
        day_copy = dict(day)
        meals_out: list[dict] = []
        day_cal = day_prot = day_fat = day_carb = 0.0
        for meal in day.get("meals") or []:
            meal_copy = dict(meal)
            ingredients = meal_copy.get("ingredients") or []
            m_cal = m_prot = m_fat = m_carb = 0.0
            enriched_ings: list[dict] = []
            for ing in ingredients:
                if not isinstance(ing, dict):
                    continue
                name = str(ing.get("name") or "").strip()
                grams = float(ing.get("grams_g") or ing.get("grams") or 0)
                if not name or grams <= 0:
                    continue
                macros = resolve_macros_per_100g(db, name)
                cal, prot, fat, carb = _scale(macros, grams)
                m_cal += cal
                m_prot += prot
                m_fat += fat
                m_carb += carb
                enriched_ings.append(
                    {
                        "name": name,
                        "grams_g": round(grams, 1),
                        "calories": round(cal),
                        "protein_g": prot,
                        "fat_g": fat,
                        "carbs_g": carb,
                    }
                )
            if enriched_ings:
                meal_copy["ingredients"] = enriched_ings
                meal_copy["calories"] = int(round(m_cal))
                meal_copy["protein_g"] = round(m_prot, 1)
                meal_copy["fat_g"] = round(m_fat, 1)
                meal_copy["carbs_g"] = round(m_carb, 1)
            day_cal += float(meal_copy.get("calories") or 0)
            day_prot += float(meal_copy.get("protein_g") or 0)
            day_fat += float(meal_copy.get("fat_g") or 0)
            day_carb += float(meal_copy.get("carbs_g") or 0)
            meals_out.append(meal_copy)
        day_copy["meals"] = meals_out
        day_copy["total_calories"] = int(round(day_cal))
        day_copy["total_protein"] = round(day_prot, 1)
        day_copy["total_fat"] = round(day_fat, 1)
        day_copy["total_carbs"] = round(day_carb, 1)
        out.append(day_copy)
    return out
