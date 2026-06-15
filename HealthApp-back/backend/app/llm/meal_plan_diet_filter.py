"""Пост-обработка плана питания с учётом вегетарианства и ограничений."""

from __future__ import annotations

from app.llm.meal_plan_fallback import VEGETARIAN_SWAPS, _swap_vegetarian

MEAT_KEYWORDS = (
    "курин",
    "индейк",
    "говядин",
    "свинин",
    "баранин",
    "утк",
    "гус",
    "бекон",
    "ветчин",
    "колбас",
    "сосиск",
    "стейк",
    "филе",
    "мясо",
    "мяс",
    "бужен",
    "шашлык",
    "бургер",
    "фарш",
    "пельмен",
    "котлет",
)

FISH_KEYWORDS = (
    "рыб",
    "лосос",
    "тунец",
    "треск",
    "сельд",
    "скумбр",
    "форел",
    "карп",
    "окун",
    "кревет",
    "кальмар",
    "мидии",
    "осьмин",
    "краб",
    "устриц",
    "морепродукт",
    "икр",
    "анчоус",
)


def profile_implies_vegetarian(is_vegetarian: bool | None, allergies_text: str | None) -> bool:
    if is_vegetarian:
        return True
    if not allergies_text:
        return False
    lower = allergies_text.lower()
    return any(
        token in lower
        for token in (
            "вегетариан",
            "vegetarian",
            "vegan",
            "веган",
            "без мяса",
        )
    )


def _contains_meat_or_fish(text: str) -> bool:
    lower = text.lower()
    return any(k in lower for k in MEAT_KEYWORDS + FISH_KEYWORDS)


def _sanitize_meal(meal: dict) -> dict:
    name = str(meal.get("name") or "")
    recipe = str(meal.get("recipe") or "")
    if not _contains_meat_or_fish(name) and not _contains_meat_or_fish(recipe):
        return meal
    cal = int(meal.get("calories") or 350)
    prot = float(meal.get("protein_g") or 18)
    fat = float(meal.get("fat_g") or 10)
    carbs = float(meal.get("carbs_g") or 40)
    new_name, new_recipe, new_cal, new_prot, new_fat, new_carbs = _swap_vegetarian(
        name, recipe, cal, prot, fat, carbs
    )
    if _contains_meat_or_fish(new_name) or _contains_meat_or_fish(new_recipe):
        new_name = "Овощное рагу с бобовыми"
        new_recipe = "Тушите сезонные овощи с нутом или чечевицей."
        new_cal, new_prot, new_fat, new_carbs = 360, 16, 8, 48
    updated = dict(meal)
    updated["name"] = new_name
    updated["recipe"] = new_recipe
    updated["calories"] = new_cal
    updated["protein_g"] = new_prot
    updated["fat_g"] = new_fat
    updated["carbs_g"] = new_carbs
    return updated


def apply_dietary_filters(
    data: dict,
    *,
    is_vegetarian: bool | None,
    allergies_text: str | None,
) -> dict:
    if not profile_implies_vegetarian(is_vegetarian, allergies_text):
        return data

    days = data.get("days") or []
    for day in days:
        meals = day.get("meals") or []
        day["meals"] = [_sanitize_meal(m) for m in meals]
        day["total_calories"] = sum(int(m.get("calories") or 0) for m in day["meals"])
        day["total_protein"] = round(sum(float(m.get("protein_g") or 0) for m in day["meals"]), 1)
        day["total_fat"] = round(sum(float(m.get("fat_g") or 0) for m in day["meals"]), 1)
        day["total_carbs"] = round(sum(float(m.get("carbs_g") or 0) for m in day["meals"]), 1)

    grocery = data.get("grocery_list") or []
    filtered_grocery = []
    for item in grocery:
        name = str(item.get("name") or "").lower()
        category = str(item.get("category") or "").lower()
        if _contains_meat_or_fish(name) or _contains_meat_or_fish(category):
            continue
        if any(k in name for k in ("куриц", "индейк", "говядин", "рыб", "лосос", "кревет")):
            continue
        filtered_grocery.append(item)
    if not filtered_grocery:
        filtered_grocery = [
            {"category": "Овощи", "name": "Сезонные овощи", "amount": "1 кг"},
            {"category": "Белок", "name": "Тофу / бобовые", "amount": "по плану"},
        ]
    data["days"] = days
    data["grocery_list"] = filtered_grocery
    return data
