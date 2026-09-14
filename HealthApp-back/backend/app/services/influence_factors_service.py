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
    "low_hydration_low_energy": ("energy", "Энергия"),
    "short_sleep_low_energy": ("energy", "Энергия"),
    "hydration_activity_energy_positive": ("energy", "Энергия"),
}

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
        metric_key, metric_title = _AFFECTED_METRIC.get(insight_type, ("other", "Самочувствие"))

        evidence = item.get("evidence") or []
        if not evidence and item.get("evidence_json"):
            try:
                evidence = json.loads(item["evidence_json"])
            except (ValueError, TypeError):
                evidence = []

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
                "comparison": _comparison_from_evidence(evidence),
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
