"""Список покупок из блюд плана — реэкспорт для совместимости."""

from app.llm.meal_plan_portions import build_grocery_list_from_days

__all__ = ["build_grocery_list_from_days"]
