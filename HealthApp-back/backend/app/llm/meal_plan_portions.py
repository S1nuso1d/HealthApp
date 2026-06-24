"""Граммовка блюд и список покупок с реальными количествами."""

from __future__ import annotations

import re

INGREDIENT_CATEGORIES: dict[str, str] = {
    "гречка": "Крупы",
    "овсянка": "Крупы",
    "рис": "Крупы",
    "манка": "Крупы",
    "макарон": "Крупы",
    "паста": "Крупы",
    "хлеб": "Хлеб",
    "курица": "Мясо",
    "куриц": "Мясо",
    "индейка": "Мясо",
    "говядина": "Мясо",
    "свинина": "Мясо",
    "фарш": "Мясо",
    "котлет": "Мясо",
    "рыба": "Рыба",
    "лосось": "Рыба",
    "минтай": "Рыба",
    "треска": "Рыба",
    "творог": "Молочное",
    "сыр": "Молочное",
    "молоко": "Молочное",
    "кефир": "Молочное",
    "сметана": "Молочное",
    "йогурт": "Молочное",
    "яйц": "Яйца",
    "омлет": "Яйца",
    "яичниц": "Яйца",
    "овощ": "Овощи",
    "картоф": "Овощи",
    "морков": "Овощи",
    "капуст": "Овощи",
    "помидор": "Овощи",
    "огурец": "Овощи",
    "борщ": "Овощи",
    "салат": "Овощи",
    "суп": "Овощи",
    "банан": "Фрукты",
    "яблок": "Фрукты",
    "тофу": "Белок",
    "чечевиц": "Белок",
    "нут": "Белок",
    "фасол": "Белок",
}

KNOWN_DISH_PORTIONS: dict[str, list[tuple[str, float]]] = {
    "гречка с курицей": [("Гречка", 150), ("Курица", 120)],
    "рис с индейкой": [("Рис", 180), ("Индейка", 110)],
    "овсянка на молоке": [("Овсянка", 50), ("Молоко", 200)],
    "рисовая каша": [("Рис", 60), ("Молоко", 200)],
    "манная каша": [("Манка", 40), ("Молоко", 200)],
    "гречка с маслом": [("Гречка", 180), ("Масло", 10)],
    "макароны с фаршем": [("Макароны", 200), ("Фарш", 100)],
    "котлеты с пюре": [("Котлеты", 150), ("Картофель", 250)],
    "рыба с картофелем": [("Рыба", 130), ("Картофель", 200)],
    "куриный суп с лапшой": [("Курица", 80), ("Лапша", 60), ("Овощи", 150)],
    "борщ со сметаной": [("Овощи для борща", 300), ("Сметана", 30)],
    "омлет с хлебом": [("Яйца", 100), ("Хлеб", 40)],
    "яичница с хлебом": [("Яйца", 100), ("Хлеб", 40)],
    "творог со сметаной": [("Творог", 150), ("Сметана", 30)],
    "творог с бананом": [("Творог", 150), ("Банан", 100)],
    "кефир и хлеб": [("Кефир", 250), ("Хлеб", 40)],
    "салат с яйцом": [("Овощи", 200), ("Яйца", 50), ("Масло", 10)],
    "овощной суп": [("Овощи", 300), ("Картофель", 80)],
}

INGREDIENT_DEFAULT_GRAMS: dict[str, float] = {
    "гречка": 150,
    "рис": 180,
    "овсянка": 50,
    "манка": 40,
    "макароны": 200,
    "лапша": 60,
    "паста": 200,
    "курица": 120,
    "куриц": 120,
    "индейка": 110,
    "говядина": 100,
    "свинина": 100,
    "фарш": 100,
    "котлеты": 150,
    "рыба": 130,
    "лосось": 120,
    "творог": 150,
    "сырники": 180,
    "омлет": 150,
    "яйца": 100,
    "яичница": 100,
    "молоко": 200,
    "кефир": 250,
    "сметана": 30,
    "йогурт": 150,
    "хлеб": 40,
    "картофель": 200,
    "овощи": 200,
    "салат": 180,
    "борщ": 350,
    "суп": 300,
    "банан": 100,
    "масло": 10,
    "тофу": 120,
    "чечевица": 150,
}

MEAL_TYPE_PORTION_G: dict[str, float] = {
    "завтрак": 220,
    "обед": 380,
    "ужин": 280,
}


def _normalize_key(text: str) -> str:
    return re.sub(r"\s+", " ", text.strip().lower())


def categorize_ingredient(name: str) -> str:
    lower = name.lower()
    for key, category in INGREDIENT_CATEGORIES.items():
        if key in lower:
            return category
    return "Прочее"


def _default_grams_for_part(part: str, meal_type: str) -> float:
    lower = part.lower().strip()
    for key, grams in INGREDIENT_DEFAULT_GRAMS.items():
        if key in lower:
            return grams
    mt = meal_type.lower()
    for key, grams in MEAL_TYPE_PORTION_G.items():
        if key in mt:
            return grams
    return 200


def _split_dish_name(name: str) -> list[str]:
    normalized = re.sub(r"\s+", " ", name.strip())
    parts = re.split(r"\s+(?:с|и|на)\s+", normalized, flags=re.IGNORECASE)
    cleaned = [p.strip(" ,.") for p in parts if p.strip(" ,.")]
    return cleaned if len(cleaned) > 1 else [normalized]


def _title_ingredient(part: str) -> str:
    part = part.strip()
    if not part:
        return part
    return part[0].upper() + part[1:]


def infer_ingredients_from_meal(meal: dict) -> list[dict]:
    existing = meal.get("ingredients") or []
    parsed: list[dict] = []
    for item in existing:
        if not isinstance(item, dict):
            continue
        name = str(item.get("name") or "").strip()
        grams = float(item.get("grams_g") or item.get("grams") or 0)
        if name and grams > 0:
            parsed.append({"name": _title_ingredient(name), "grams_g": round(grams, 1)})
    if parsed:
        return parsed

    dish_name = str(meal.get("name") or "").strip()
    meal_type = str(meal.get("meal_type") or "")
    key = _normalize_key(dish_name)
    if key in KNOWN_DISH_PORTIONS:
        return [{"name": n, "grams_g": float(g)} for n, g in KNOWN_DISH_PORTIONS[key]]

    parts = _split_dish_name(dish_name)
    if len(parts) > 1:
        return [
            {
                "name": _title_ingredient(part),
                "grams_g": round(_default_grams_for_part(part, meal_type), 1),
            }
            for part in parts
        ]

    return [
        {
            "name": _title_ingredient(dish_name),
            "grams_g": round(_default_grams_for_part(dish_name, meal_type), 1),
        }
    ]


def enrich_meal_plan_days(days: list[dict]) -> list[dict]:
    enriched: list[dict] = []
    for day in days:
        day_copy = dict(day)
        meals_out: list[dict] = []
        for meal in day.get("meals") or []:
            meal_copy = dict(meal)
            meal_copy["ingredients"] = infer_ingredients_from_meal(meal_copy)
            meals_out.append(meal_copy)
        day_copy["meals"] = meals_out
        enriched.append(day_copy)
    return enriched


def format_amount(name: str, grams: float) -> str:
    lower = name.lower()
    if any(k in lower for k in ("яйц", "яичниц", "омлет")) and grams >= 40:
        count = max(1, round(grams / 50))
        return f"{count} шт"
    if any(k in lower for k in ("молоко", "кефир", "йогурт", "бульон")):
        return f"{int(round(grams))} мл"
    if grams >= 1000:
        kg = grams / 1000
        if kg >= 10:
            return f"{int(round(kg))} кг"
        text = f"{kg:.1f}".rstrip("0").rstrip(".")
        return f"{text} кг"
    return f"{int(round(grams))} г"


def build_grocery_list_from_days(days: list[dict]) -> list[dict]:
    totals: dict[tuple[str, str], float] = {}

    for day in days:
        for meal in day.get("meals") or []:
            ingredients = meal.get("ingredients") or infer_ingredients_from_meal(meal)
            for ing in ingredients:
                name = str(ing.get("name") or "").strip()
                grams = float(ing.get("grams_g") or ing.get("grams") or 0)
                if not name or grams <= 0:
                    continue
                category = categorize_ingredient(name)
                key = (category, name)
                totals[key] = totals.get(key, 0.0) + grams

    grocery_list = [
        {
            "category": category,
            "name": name,
            "amount": format_amount(name, grams),
        }
        for (category, name), grams in sorted(totals.items(), key=lambda x: (x[0][0], x[0][1]))
    ]

    if not grocery_list:
        plan_days = len(days)
        grocery_list = [
            {"category": "Овощи", "name": "Сезонные овощи", "amount": f"{plan_days * 300} г"},
            {"category": "Белок", "name": "Основной источник белка", "amount": f"{plan_days * 120} г"},
        ]
    return grocery_list
