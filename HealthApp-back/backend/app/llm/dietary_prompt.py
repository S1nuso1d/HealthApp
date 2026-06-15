"""Правила питания для AI-промптов и пост-фильтрация ответов."""

from __future__ import annotations

from app.llm.meal_plan_diet_filter import MEAT_KEYWORDS, FISH_KEYWORDS, profile_implies_vegetarian


VEGETARIAN_PROMPT_RULES = """
ОГРАНИЧЕНИЯ ПИТАНИЯ (ОБЯЗАТЕЛЬНО):
- Если в профиле вегетарианство — НИКОГДА не предлагайте мясо, птицу, рыбу, морепродукты, бульоны на мясе/рыбе.
- Запрещённые продукты: курица, говядина, свинина, индейка, утка, бекон, колбаса, рыба, креветки, тунец, лосось, икра.
- Если указаны аллергии/исключения в профиле — не включайте эти продукты ни в рекомендации, ни в примеры блюд.
""".strip()


def build_dietary_rules_block(
    *,
    is_vegetarian: bool | None,
    allergies_text: str | None,
) -> str:
    parts: list[str] = []
    if profile_implies_vegetarian(is_vegetarian, allergies_text):
        parts.append(VEGETARIAN_PROMPT_RULES)
    if allergies_text and allergies_text.strip():
        parts.append(f"Аллергии и исключения пользователя: {allergies_text.strip()}")
    return "\n\n".join(parts)


def contains_meat_or_fish(text: str) -> bool:
    lower = text.lower()
    return any(k in lower for k in MEAT_KEYWORDS + FISH_KEYWORDS)


def filter_food_items(
    items: list[dict],
    *,
    is_vegetarian: bool | None,
    allergies_text: str | None,
) -> list[dict]:
    if not profile_implies_vegetarian(is_vegetarian, allergies_text):
        return items
    filtered: list[dict] = []
    for item in items:
        name = str(item.get("name") or "")
        if contains_meat_or_fish(name):
            continue
        filtered.append(item)
    return filtered
