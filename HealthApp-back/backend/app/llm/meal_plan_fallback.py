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
        ("Завтрак", "Овсянка с ягодами", "Залейте овсянку, добавьте ягоды и орехи.", 380, 14, 10, 58),
        ("Обед", "Куриная грудка с овощами", "Запеките курицу с овощами на сковороде.", 420, 38, 12, 35),
        ("Ужин", "Творог с фруктами", "Смешайте творог с фруктами.", 340, 28, 8, 28),
    ],
    [
        ("Завтрак", "Яичница с помидорами", "Обжарьте яйца с помидорами и зеленью.", 360, 22, 18, 12),
        ("Обед", "Индейка с гречкой", "Отварите гречку, подайте с индейкой.", 440, 36, 10, 42),
        ("Ужин", "Салат с тунцом", "Смешайте тунец, овощи и оливковое масло.", 320, 30, 14, 18),
    ],
    [
        ("Завтрак", "Греческий йогурт с мёдом", "Йогурт, мёд, орехи и банан.", 350, 18, 8, 48),
        ("Обед", "Лосось с рисом", "Запеките лосось, подайте с рисом и брокколи.", 480, 34, 16, 45),
        ("Ужин", "Овощной суп с фасолью", "Сварите суп из овощей и белой фасоли.", 300, 16, 6, 42),
    ],
    [
        ("Завтрак", "Тост с авокадо и яйцом", "Поджарьте хлеб, добавьте авокадо и яйцо.", 390, 16, 20, 32),
        ("Обед", "Говядина тушёная с картофелем", "Тушите говядину с овощами 40 мин.", 450, 32, 14, 40),
        ("Ужин", "Кефир и орехи", "Стакан кефира и горсть орехов.", 280, 14, 16, 18),
    ],
    [
        ("Завтрак", "Сырники со сметаной", "Обжарьте сырники на сухой сковороде.", 400, 24, 14, 38),
        ("Обед", "Паста с креветками", "Отварите пасту, обжарьте с креветками и чесноком.", 460, 28, 12, 52),
        ("Ужин", "Запечённые овощи с хумусом", "Запеките овощи, подайте с хумусом.", 310, 12, 10, 38),
    ],
    [
        ("Завтрак", "Смузи с бананом и шпинатом", "Взбейте банан, шпинат, йогурт и овсянку.", 340, 16, 6, 52),
        ("Обед", "Куриный суп с лапшой", "Сварите лёгкий бульон с курицей и овощами.", 380, 26, 8, 44),
        ("Ужин", "Омлет с грибами", "Обжарьте грибы, залейте яйцами.", 330, 24, 18, 8),
    ],
    [
        ("Завтрак", "Мюсли с молоком", "Залейте мюсли молком, добавьте яблоко.", 370, 12, 8, 62),
        ("Обед", "Рыба на пару с киноа", "Приготовьте рыбу на пару с киноа и салатом.", 430, 36, 12, 40),
        ("Ужин", "Тёплый салат с индейкой", "Индейка, руккола, запечённые овощи.", 350, 32, 10, 22),
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
            "вегетарианство",
            "вегетариан",
            "vegetarian",
            "vegan",
            "веган",
            "строго вегетариан",
            "без мяса",
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


def build_fallback_meal_plan_json(
    analytics: AnalyticsResponse,
    days: int = 7,
    user_context: str | None = None,
) -> str:
    is_vegetarian, allergens = _parse_context_flags(user_context)
    day_names = DAY_NAMES[:7]

    target_calories = 1800
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
        total_cal = sum(m["calories"] for m in meals)
        scale = target_calories / total_cal if total_cal > 0 else 1.0
        if abs(scale - 1.0) > 0.08:
            for m in meals:
                m["calories"] = int(m["calories"] * scale)

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

    grocery: dict[str, set[str]] = {}
    for day in days_payload:
        for meal in day["meals"]:
            name = meal["name"].lower()
            if "овсян" in name or "мюсли" in name:
                grocery.setdefault("Крупы", set()).add("Овсянка / мюсли")
            if "рис" in name or "греч" in name or "киноа" in name or "паста" in name:
                grocery.setdefault("Крупы", set()).add("Рис / гречка / киноа")
            if any(k in name for k in ("куриц", "индейк", "говядин")):
                grocery.setdefault("Белок", set()).add("Мясо птицы / говядина")
            if any(k in name for k in ("лосос", "рыб", "тунец", "кревет")):
                grocery.setdefault("Белок", set()).add("Рыба / морепродукты")
            if any(k in name for k in ("тофu", "тофу", "чечев", "нут", "фасол")):
                grocery.setdefault("Белок", set()).add("Бобовые / тофу")
            if "овощ" in meal["recipe"].lower() or "салат" in name:
                grocery.setdefault("Овощи", set()).add("Смесь овощей")
            if any(k in name for k in ("творог", "йогурт", "сыр", "кефир", "молок")):
                grocery.setdefault("Молочное", set()).add("Молочные продукты")
            if any(k in name for k in ("яйц", "омлет", "яичн")):
                grocery.setdefault("Яйца", set()).add("Яйца")

    if is_vegetarian:
        grocery.pop("Белок", None)
        grocery.setdefault("Белок", set()).add("Тофу / бобовые")

    grocery_list = [
        {"category": cat, "name": item, "amount": "по плану"}
        for cat, items in grocery.items()
        for item in sorted(items)
    ]
    if not grocery_list:
        grocery_list = [
            {"category": "Овощи", "name": "Сезонные овощи", "amount": "1 кг"},
            {"category": "Белок", "name": "Основной источник белка", "amount": "по плану"},
        ]

    return json.dumps({"days": days_payload, "grocery_list": grocery_list}, ensure_ascii=False)
