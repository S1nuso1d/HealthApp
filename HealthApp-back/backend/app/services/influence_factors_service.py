"""Факторы влияния для экрана «что на тебя влияет».

`CorrelationAnalyzer` уже находит связи вроде «поздний кофеин ↔ короткий сон»,
но отдаёт их как текстовые инсайты с сырыми evidence-парами. Здесь они
приводятся к форме, из которой интерфейс может построить список факторов
с силой влияния и наглядное сравнение «в дни с X» против «в дни без X».
"""

from __future__ import annotations

import json

from sqlalchemy.orm import Session

from app.services.correlation_analyzer import CorrelationAnalyzer

# На что влияет фактор и как называется затронутая метрика в интерфейсе.
_AFFECTED_METRIC = {
    "late_caffeine_sleep_impact": ("sleep", "Сон"),
    "late_meal_sleep_impact": ("sleep", "Сон"),
    "evening_high_activity_sleep_impact": ("sleep", "Сон"),
    "late_drink_sleep_impact": ("sleep", "Сон"),
    "high_daily_caffeine_sleep_impact": ("sleep", "Сон"),
    "late_meal_and_evening_workout_sleep": ("sleep", "Сон"),
    "late_caffeine_and_late_meal_sleep": ("sleep", "Сон"),
    "late_meal_sleep_quality_impact": ("sleep", "Сон"),
    "evening_walk_sleep_positive": ("sleep", "Сон"),
    "low_hydration_low_energy": ("energy", "Энергия"),
    "short_sleep_low_energy": ("energy", "Энергия"),
    "hydration_activity_energy_positive": ("energy", "Энергия"),
    "low_steps_low_energy": ("energy", "Энергия"),
    "high_steps_high_energy": ("energy", "Энергия"),
    "skipped_meals_low_energy": ("energy", "Энергия"),
    "good_sleep_high_energy": ("energy", "Энергия"),
    "low_hydration_high_activity_energy": ("energy", "Энергия"),
    "recovery_stack_high_energy": ("energy", "Энергия"),
    "short_sleep_low_mood": ("mood", "Настроение"),
    "short_sleep_high_stress": ("stress", "Стресс"),
    "low_hydration_low_focus": ("focus", "Фокус"),
}


def _metric_for(insight_type: str) -> tuple[str, str]:
    if insight_type in _AFFECTED_METRIC:
        return _AFFECTED_METRIC[insight_type]
    if "sleep" in insight_type:
        return ("sleep", "Сон")
    if "mood" in insight_type:
        return ("mood", "Настроение")
    if "stress" in insight_type:
        return ("stress", "Стресс")
    if "focus" in insight_type:
        return ("focus", "Фокус")
    if "energy" in insight_type:
        return ("energy", "Энергия")
    return ("other", "Самочувствие")

# Единица измерения -> как подписать значение.
_UNIT_LABELS = {
    "hours": "ч",
    "hour_of_day": "ч",
    "ml": "мл",
    "mg": "мг",
    "steps": "шагов",
    "points": "балл",
}

_SEVERITY_WEIGHT = {"low": 0.6, "medium": 0.8, "high": 1.0}


def _comparison_from_evidence(evidence: list[dict]) -> dict | None:
    """Собрать пару «с фактором / без фактора» из evidence-записей.

    Анализатор кладёт значения парами с говорящими именами метрик
    (`..._with_late_meal` / `..._without_late_meal`), поэтому опираемся
    на наличие `without` в имени, а не на порядок.
    """
    with_entry = None
    without_entry = None
    for entry in evidence:
        metric = str(entry.get("metric", ""))
        if "without" in metric or "normal" in metric:
            without_entry = without_entry or entry
        else:
            with_entry = with_entry or entry

    if with_entry is None or without_entry is None:
        return None

    unit = with_entry.get("unit") or without_entry.get("unit")
    return {
        "with_factor_value": with_entry.get("value"),
        "with_factor_note": with_entry.get("note"),
        "without_factor_value": without_entry.get("value"),
        "without_factor_note": without_entry.get("note"),
        "unit": unit,
        "unit_label": _UNIT_LABELS.get(unit or "", unit),
    }


def _fmt_num(value) -> str:
    try:
        number = float(value)
    except (TypeError, ValueError):
        return str(value)
    if abs(number - round(number)) < 0.05:
        return str(int(round(number)))
    return f"{number:.1f}"


def _proof_line(comparison: dict, metric_title: str, with_n: int | None, without_n: int | None) -> str | None:
    with_v = comparison.get("with_factor_value")
    without_v = comparison.get("without_factor_value")
    if with_v is None or without_v is None:
        return None
    unit = comparison.get("unit_label") or ""
    with_txt = f"{_fmt_num(with_v)} {unit}".strip()
    without_txt = f"{_fmt_num(without_v)} {unit}".strip()
    with_days = f"В {with_n} днях" if with_n else "В дни"
    without_days = f"в {without_n} днях" if without_n else "в дни"
    metric = (metric_title or "показатель").lower()
    return (
        f"{with_days} с фактором {metric} {with_txt}, "
        f"{without_days} без него — {without_txt}."
    )


def _strength(confidence: float | None, severity: str | None) -> int:
    """Сила влияния 0–100 для полоски в интерфейсе.

    Отдельного «размера эффекта» анализатор не возвращает, поэтому берём
    уверенность и приглушаем её весом severity: слабая связь с высокой
    уверенностью не должна выглядеть так же весомо, как сильная.
    """
    base = float(confidence or 0.0)
    weight = _SEVERITY_WEIGHT.get((severity or "").strip().lower(), 0.8)
    return max(0, min(100, round(base * weight * 100)))


def build_influence_factors(
    db: Session,
    user_id: int,
    period_days: int = 14,
) -> dict:
    raw = CorrelationAnalyzer.analyze_correlations(
        db=db,
        user_id=user_id,
        period_days=period_days,
    )

    factors = []
    for item in raw:
        insight_type = item.get("insight_type", "")
        metric_key, metric_title = _metric_for(insight_type)

        evidence = item.get("evidence") or []
        if not evidence and item.get("evidence_json"):
            try:
                evidence = json.loads(item["evidence_json"])
            except (ValueError, TypeError):
                evidence = []

        comparison = _comparison_from_evidence(evidence)
        if comparison:
            with_n = item.get("sample_with")
            without_n = item.get("sample_without")
            comparison["with_days"] = with_n
            comparison["without_days"] = without_n
            comparison["proof_line"] = _proof_line(
                comparison, metric_title, with_n, without_n
            )

        factors.append(
            {
                "id": insight_type,
                "title": item.get("title"),
                "description": item.get("description"),
                "impact": item.get("impact", "neutral"),
                "confidence": item.get("confidence"),
                "severity": item.get("severity"),
                "strength": _strength(item.get("confidence"), item.get("severity")),
                "affects_metric": metric_key,
                "affects_metric_title": metric_title,
                "suggested_action": (
                    "7 дней без этого фактора — потом сравним сон и энергию."
                    if item.get("impact") != "positive"
                    else "Повторите дни, когда этот паттерн уже работал."
                ),
                "comparison": comparison,
            }
        )

    # Сильные связи наверх — экран должен начинаться с того, что важнее всего.
    factors.sort(key=lambda f: f["strength"], reverse=True)

    return {
        "period_days": period_days,
        "factors": factors,
        # Порог тот же, что у анализатора: пары дней «с фактором» и «без» должны
        # набраться хотя бы по две, иначе связей просто не будет найдено.
        "has_enough_data": bool(factors),
        "message": (
            None
            if factors
            else "Пока мало данных, чтобы найти закономерности. "
            "Нужно хотя бы две недели записей сна, еды и воды."
        ),
    }
