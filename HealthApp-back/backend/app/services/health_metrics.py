"""Чистые функции метрик здоровья без зависимостей от других сервисов (избегаем циклических импортов)."""


def clamp_score(value: float) -> int:
    return max(0, min(100, round(value)))


def normalize_mood_value(mood: float) -> float:
    """В UI настроение 1–5, энергия/стресс 1–10 — приводим настроение к шкале 10."""
    if mood <= 5:
        return mood * 2.0
    return mood
