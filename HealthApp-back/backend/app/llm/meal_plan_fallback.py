"""Шаблонный план питания без LLM — разнообразие по дням и учёт аллергий."""

from __future__ import annotations

import hashlib
import json
import re
from datetime import date

from app.schemas.analytics import AnalyticsResponse

DAY_NAMES = [
    "Понедельник",
    "Вторник",
    "Среда",
    "Четверг",
    "Пятница",
    "Суббота",
    "Воскресенье",
]

# (meal_type, name, recipe, calories, protein, fat, carbs)
MEAL_TEMPLATES: list[list[tuple]] = [
    [
        ("Завтрак", "Овсянка на молоке", "Сварите кашу, добавьте масло.", 360, 12, 8, 58),
        ("Обед", "Гречка с курицей", "Отварите гречку, поджарьте курицу.", 480, 38, 12, 52),
        ("Ужин", "Творог со сметаной", "Порция творога со сметаной.", 300, 26, 8, 20),
    ],
    [
        ("Завтрак", "Омлет с хлебом", "Яйца на сковороде, тост.", 370, 20, 16, 28),
        ("Обед", "Борщ со сметаной", "Порция борща, хлеб.", 420, 16, 14, 48),
        ("Ужин", "Кефир и хлеб", "Стакан кефира с бутербродом.", 280, 12, 8, 32),
    ],
    [
        ("Завтрак", "Рисовая каша", "Рис на молоке с сахаром.", 340, 8, 6, 62),
        ("Обед", "Макароны с фаршем", "Отварите макароны, обжарьте фарш.", 510, 28, 16, 58),
        ("Ужин", "Салат с яйцом", "Яйцо, огурец, помидор, масло.", 290, 14, 18, 12),
    ],
    [
        ("Завтрак", "Сырники", "Обжарьте сырники на сковороде.", 390, 22, 14, 36),
        ("Обед", "Куриный суп с лапшой", "Сварите суп с курицей.", 380, 24, 8, 42),
        ("Ужин", "Гречка с молоком", "Гречка с молоком.", 310, 12, 6, 48),
    ],
    [
        ("Завтрак", "Яичница с хлебом", "Яйца, хлеб, чай.", 350, 18, 14, 30),
        ("Обед", "Рис с индейкой", "Рис и тушёная индейка.", 460, 36, 10, 50),
        ("Ужин", "Овощной суп", "Лёгкий суп из овощей.", 280, 8, 6, 40),
    ],
    [
        ("Завтрак", "Манная каша", "Каша с маслом.", 330, 10, 8, 52),
        ("Обед", "Котлеты с пюре", "Котлеты из фарша, пюре.", 520, 30, 18, 55),
        ("Ужин", "Творог с бананом", "Творог и банан.", 310, 24, 6, 36),
    ],
    [
        ("Завтрак", "Гречка с маслом", "Гречка на завтрак.", 320, 10, 8, 52),
        ("Обед", "Рыба с картофелем", "Запечённая рыба с картошкой.", 440, 32, 12, 42),
        ("Ужин", "Омлет", "Омлет на ужин.", 300, 22, 16, 6),
    ],
]

VEGETARIAN_SWAPS = {
    "курин": ("Чечевичный карри с рисом", "Сварите чечевицу с карри и овощами.", 400, 18, 8, 55),
    "индейк": ("Тофу-стирфрай с овощами", "Обжарьте тофу с овощами и соевым соусом.", 380, 22, 12, 38),
    "говядин": ("Бобовый рагу с овощами", "Тушите фасоль с овощами и травами.", 390, 20, 6, 48),
    "лосос": ("Запечённый тофу с лимоном", "Маринуйте тофу и запеките с лимоном.", 360, 24, 14, 22),
    "тунец": ("Салат с нутом и овощами", "Нут, овощи, оливковое масло.", 340, 16, 10, 40),
    "кревет": ("Паста с грибами", "Паста с шампиньонами и чесноком.", 420, 16, 10, 58),
    "рыб": ("Овощная лазанья", "Слои овощей, соуса и пасты.", 380, 16, 12, 44),
    "яичн": ("Тофу-скramble с овощами", "Обжарьте тофу с куркумой и овощами.", 320, 20, 14, 18),
    "омлет": ("Тофу-скramble с грибами", "Тофу с грибами и зеленью.", 310, 22, 14, 12),
    "сырник": ("Овсяные панкейки", "Овсяная мука, банан, яйцо или льняное яйцо.", 360, 12, 8, 52),
    "творог": ("Сояный йогурт с фруктами", "Йогурт на сое с ягодами.", 300, 14, 6, 38),
}


def _parse_context_flags(user_context: str | None) -> tuple[bool, list[str]]:
    if not user_context:
        return False, []
    lower = user_context.lower()
    is_vegetarian = any(
        token in lower
        for token in (
            "вегетарианство: да",
            "вегетарианское меню",
            "строго вегетариан",
        )
    )
    allergies: list[str] = []
    for line in user_context.splitlines():
        if "аллерг" in line.lower() or "ограничен" in line.lower():
            _, _, tail = line.partition(":")
            if tail.strip():
                parts = re.split(r"[,;]+", tail)
                allergies.extend(p.strip().lower() for p in parts if p.strip())
    return is_vegetarian, allergies


def _contains_allergen(text: str, allergens: list[str]) -> bool:
    lower = text.lower()
    return any(a in lower or lower in a for a in allergens if len(a) >= 2)


def _swap_vegetarian(name: str, recipe: str, calories: int, protein: float, fat: float, carbs: float):
    lower = name.lower()
    for key, swap in VEGETARIAN_SWAPS.items():
        if key in lower:
            return swap[0], swap[1], swap[2], swap[3], swap[4], swap[5]
    return name, recipe, calories, protein, fat, carbs


def _pick_meal(
    template: tuple,
    is_vegetarian: bool,
    allergens: list[str],
    day_offset: int,
    meal_index: int,
) -> dict:
    meal_type, name, recipe, cal, prot, fat, carbs = template
    if is_vegetarian:
        name, recipe, cal, prot, fat, carbs = _swap_vegetarian(name, recipe, cal, prot, fat, carbs)

    alt_templates = MEAL_TEMPLATES[(day_offset + meal_index + 1) % len(MEAL_TEMPLATES)]
    alt = alt_templates[meal_index % len(alt_templates)]
    attempts = 0
    while _contains_allergen(name, allergens) and attempts < len(MEAL_TEMPLATES):
        alt = MEAL_TEMPLATES[(day_offset + meal_index + attempts + 2) % len(MEAL_TEMPLATES)][meal_index % 3]
        meal_type, name, recipe, cal, prot, fat, carbs = alt
        if is_vegetarian:
            name, recipe, cal, prot, fat, carbs = _swap_vegetarian(name, recipe, cal, prot, fat, carbs)
        attempts += 1

    return {
        "meal_type": meal_type,
        "name": name,
        "calories": cal,
        "protein_g": float(prot),
        "fat_g": float(fat),
        "carbs_g": float(carbs),
        "recipe": recipe,
    }


def _parse_kbju_targets(user_context: str | None) -> tuple[int, float, float, float] | None:
    if not user_context:
        return None
    for line in user_context.splitlines():
        if "цель кбжу" in line.lower():
            nums = re.findall(r"[\d.]+", line)
            if len(nums) >= 4:
                return int(float(nums[0])), float(nums[1]), float(nums[2]), float(nums[3])
    return None


def _scale_meals_to_targets(
    meals: list[dict],
    target_cal: int,
    target_prot: float,
    target_fat: float,
    target_carbs: float,
) -> None:
    total_cal = sum(m["calories"] for m in meals) or 1
    total_prot = sum(m["protein_g"] for m in meals) or 1.0
    total_fat = sum(m["fat_g"] for m in meals) or 1.0
    total_carbs = sum(m["carbs_g"] for m in meals) or 1.0

    cal_scale = target_cal / total_cal
    prot_scale = target_prot / total_prot
    fat_scale = target_fat / total_fat
    carbs_scale = target_carbs / total_carbs

    for m in meals:
        m["calories"] = max(80, int(m["calories"] * cal_scale))
        m["protein_g"] = round(m["protein_g"] * prot_scale, 1)
        m["fat_g"] = round(m["fat_g"] * fat_scale, 1)
        m["carbs_g"] = round(m["carbs_g"] * carbs_scale, 1)

    # Подгоняем калории к цели на самом крупном приёме пищи
    diff = target_cal - sum(m["calories"] for m in meals)
    if meals and diff != 0:
        largest = max(meals, key=lambda x: x["calories"])
        largest["calories"] = max(80, largest["calories"] + diff)


def build_fallback_meal_plan_json(
    analytics: AnalyticsResponse,
    days: int = 7,
    user_context: str | None = None,
    *,
    is_vegetarian: bool | None = None,
) -> str:
    if is_vegetarian is None:
        is_vegetarian, allergens = _parse_context_flags(user_context)
    else:
        is_vegetarian = bool(is_vegetarian)
        _, allergens = _parse_context_flags(user_context)
    day_names = DAY_NAMES[:7]

    kbju = _parse_kbju_targets(user_context)
    if kbju:
        target_calories, target_protein, target_fat, target_carbs = kbju
    else:
        target_calories = 1800
        target_protein = 100.0
        target_fat = 60.0
        target_carbs = 200.0
        if analytics.summary.nutrition_score > 0:
            target_calories = max(1500, min(2400, int(1800 + (analytics.summary.nutrition_score - 50) * 4)))

    seed = int(hashlib.md5(date.today().isoformat().encode()).hexdigest()[:8], 16)
    rotation = seed % len(MEAL_TEMPLATES)

    days_payload = []
    for index in range(min(days, 14)):
        template_day = MEAL_TEMPLATES[(rotation + index) % len(MEAL_TEMPLATES)]
        meals = [
            _pick_meal(template_day[i], is_vegetarian, allergens, rotation + index, i)
            for i in range(min(3, len(template_day)))
        ]
        _scale_meals_to_targets(meals, target_calories, target_protein, target_fat, target_carbs)

        days_payload.append(
            {
                "day_name": day_names[index % 7],
                "meals": meals,
                "total_calories": sum(m["calories"] for m in meals),
                "total_protein": round(sum(m["protein_g"] for m in meals), 1),
                "total_fat": round(sum(m["fat_g"] for m in meals), 1),
                "total_carbs": round(sum(m["carbs_g"] for m in meals), 1),
            }
        )

    from app.llm.meal_plan_portions import build_grocery_list_from_days, enrich_meal_plan_days

    days_payload = enrich_meal_plan_days(days_payload)
    grocery_list = build_grocery_list_from_days(days_payload)

    return json.dumps({"days": days_payload, "grocery_list": grocery_list}, ensure_ascii=False)
