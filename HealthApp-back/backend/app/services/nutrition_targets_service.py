"""Расчёт суточных калорий и БЖУ по антропометрии и цели (Mifflin–St Jeor + TDEE)."""

from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class NutritionTargets:
    target_daily_calories: int
    target_protein_g: float
    target_fat_g: float
    target_carbs_g: float
    target_water_ml: float
    target_sleep_hours: float
    target_steps: int


_ACTIVITY_MULT = {
    "low": 1.375,
    "medium": 1.55,
    "high": 1.725,
}

_GOAL_CAL_FACTOR = {
    "lose_weight": 0.85,
    "gain_muscle": 1.12,
    "better_sleep": 1.0,
    "improve_energy": 1.0,
}

_PROTEIN_G_PER_KG = {
    "lose_weight": 1.8,
    "gain_muscle": 2.0,
    "better_sleep": 1.5,
    "improve_energy": 1.5,
}


def _bmr_kcal(age: int, sex: str, height_cm: float, weight_kg: float) -> float:
    base = 10.0 * weight_kg + 6.25 * height_cm - 5.0 * age
    if sex == "female":
        return base - 161.0
    return base + 5.0


def calculate_nutrition_targets(
    *,
    age: int,
    sex: str,
    height_cm: float,
    weight_kg: float,
    activity_level: str,
    goal: str,
) -> NutritionTargets:
    bmr = _bmr_kcal(age, sex, height_cm, weight_kg)
    mult = _ACTIVITY_MULT.get(activity_level, 1.55)
    tdee = bmr * mult
    cal_factor = _GOAL_CAL_FACTOR.get(goal, 1.0)
    calories = max(1200, min(8000, int(round(tdee * cal_factor))))

    protein_per_kg = _PROTEIN_G_PER_KG.get(goal, 1.5)
    protein_g = round(weight_kg * protein_per_kg, 1)
    fat_kcal = calories * 0.28
    fat_g = round(fat_kcal / 9.0, 1)
    protein_kcal = protein_g * 4.0
    carbs_kcal = max(0.0, calories - protein_kcal - fat_g * 9.0)
    carbs_g = round(carbs_kcal / 4.0, 1)

    water_ml = max(2000.0, round(weight_kg * 35.0 / 100.0) * 100.0)

    return NutritionTargets(
        target_daily_calories=calories,
        target_protein_g=protein_g,
        target_fat_g=fat_g,
        target_carbs_g=carbs_g,
        target_water_ml=water_ml,
        target_sleep_hours=8.0,
        target_steps=10_000,
    )


def try_calculate_from_profile(
    age: Optional[int],
    sex: Optional[str],
    height_cm: Optional[float],
    weight_kg: Optional[float],
    activity_level: Optional[str],
    goal: Optional[str],
) -> Optional[NutritionTargets]:
    if not all([age, sex, height_cm, weight_kg, activity_level, goal]):
        return None
    if age < 1 or height_cm <= 0 or weight_kg <= 0:
        return None
    return calculate_nutrition_targets(
        age=age,
        sex=sex,
        height_cm=height_cm,
        weight_kg=weight_kg,
        activity_level=activity_level,
        goal=goal,
    )
