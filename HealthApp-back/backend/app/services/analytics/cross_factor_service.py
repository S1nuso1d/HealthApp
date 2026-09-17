"""Кросс-факторный анализ дневника.

Помимо простых пар вроде «кофеин → сон» смотрим сочетания разделов:
поздняя тренировка и поздний ужин вместе, мало воды в день нагрузки,
короткий сон и настроение на следующий день. Каждый инсайт — сравнение
дней «с фактором» и «без», с порогом по размеру эффекта, чтобы шум
из двух случайных дней не превращался в совет.
"""

from __future__ import annotations

import json
from collections.abc import Callable
from typing import Any

from app.services.correlation_analyzer import (
    build_evidence,
    estimate_confidence,
    safe_mean,
)

DayPred = Callable[[dict[str, Any]], bool]


def _groups(
    day_rows: list[dict[str, Any]],
    pred: DayPred,
    outcome_key: str,
    *,
    require_positive: bool = False,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    with_g: list[dict[str, Any]] = []
    without_g: list[dict[str, Any]] = []
    for row in day_rows:
        value = row.get(outcome_key)
        if value is None:
            continue
        if require_positive and float(value) <= 0:
            continue
        (with_g if pred(row) else without_g).append(row)
    return with_g, without_g


def _maybe_insight(
    *,
    day_rows: list[dict[str, Any]],
    pred: DayPred,
    outcome_key: str,
    insight_type: str,
    title: str,
    unit: str,
    min_abs_diff: float,
    period_days: int,
    with_note: str,
    without_note: str,
    description: Callable[[float, float, float], str],
    higher_is_better: bool = True,
    require_positive: bool = False,
    min_each: int = 2,
    impact_when_factor_worse: str = "negative",
    base_confidence: float = 0.56,
) -> dict[str, Any] | None:
    with_g, without_g = _groups(
        day_rows, pred, outcome_key, require_positive=require_positive
    )
    if len(with_g) < min_each or len(without_g) < min_each:
        return None

    avg_with = safe_mean([row[outcome_key] for row in with_g])
    avg_without = safe_mean([row[outcome_key] for row in without_g])
    diff = avg_with - avg_without
    if abs(diff) < min_abs_diff:
        return None

    want_positive = impact_when_factor_worse == "positive"
    factor_helps = (diff > 0) if higher_is_better else (diff < 0)
    if want_positive and not factor_helps:
        return None
    if not want_positive and factor_helps:
        return None

    confidence = estimate_confidence(
        len(with_g),
        len(without_g),
        abs(diff),
        min_abs_diff,
        base=base_confidence,
    )
    severity = "high" if abs(diff) >= min_abs_diff * 2 else "medium"
    if impact_when_factor_worse == "positive":
        severity = "low"

    evidence = [
        build_evidence(
            f"{outcome_key}_with_factor",
            avg_with,
            unit,
            with_note,
        ),
        build_evidence(
            f"{outcome_key}_without_factor",
            avg_without,
            unit,
            without_note,
        ),
    ]
    return {
        "insight_type": insight_type,
        "category": "correlation",
        "title": title,
        "description": description(avg_with, avg_without, diff),
        "confidence": confidence,
        "severity": severity,
        "impact": "positive" if impact_when_factor_worse == "positive" else "negative",
        "sample_with": len(with_g),
        "sample_without": len(without_g),
        "evidence_json": json.dumps(evidence, ensure_ascii=False),
        "window_days": period_days,
    }


def discover_cross_factor_insights(
    day_rows: list[dict[str, Any]],
    period_days: int,
) -> list[dict[str, Any]]:
    """Дополнительные связи между сном, едой, водой и активностью."""
    if len(day_rows) < 4:
        return []

    found: list[dict[str, Any]] = []

    def add(item: dict[str, Any] | None) -> None:
        if item is not None:
            found.append(item)

    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: bool(d.get("late_drink")),
            outcome_key="sleep_duration",
            insight_type="late_drink_sleep_impact",
            title="Позднее питьё связано с более коротким сном",
            unit="hours",
            min_abs_diff=0.4,
            period_days=period_days,
            with_note="Сон в дни с напитком поздно вечером",
            without_note="Сон в дни без позднего питья",
            description=lambda w, n, diff: (
                f"В дни, когда напиток был поздно вечером, сон был короче на "
                f"{abs(diff):.1f} ч ({w:.1f} против {n:.1f} ч)."
            ),
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("total_caffeine_mg") or 0) >= 200,
            outcome_key="sleep_duration",
            insight_type="high_daily_caffeine_sleep_impact",
            title="Много кофеина за день связано с более коротким сном",
            unit="hours",
            min_abs_diff=0.4,
            period_days=period_days,
            with_note="Сон в дни с ≥200 мг кофеина",
            without_note="Сон в дни с меньшим кофеином",
            description=lambda w, n, diff: (
                f"В дни с 200+ мг кофеина сон был короче на {abs(diff):.1f} ч "
                f"({w:.1f} против {n:.1f} ч)."
            ),
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: int(d.get("steps") or 0) < 5000,
            outcome_key="energy",
            insight_type="low_steps_low_energy",
            title="Мало шагов связано с более низкой энергией",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Энергия в дни меньше 5000 шагов",
            without_note="Энергия в дни с большей ходьбой",
            description=lambda w, n, diff: (
                f"В дни меньше 5000 шагов энергия была ниже на {abs(diff):.1f} балла "
                f"({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: int(d.get("steps") or 0) >= 8000,
            outcome_key="energy",
            insight_type="high_steps_high_energy",
            title="Дни с 8000+ шагами связаны с более высокой энергией",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Энергия в дни с 8000+ шагами",
            without_note="Энергия в менее активные дни",
            description=lambda w, n, diff: (
                f"В дни с 8000+ шагами энергия была выше на {diff:.1f} балла "
                f"({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
            impact_when_factor_worse="positive",
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: int(d.get("meal_count") or 0) <= 1,
            outcome_key="energy",
            insight_type="skipped_meals_low_energy",
            title="Пропуски приёмов пищи связаны с падением энергии",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Энергия в дни с 0–1 приёмом пищи",
            without_note="Энергия в дни с более регулярным питанием",
            description=lambda w, n, diff: (
                f"В дни с одним приёмом пищи или без него энергия была ниже на "
                f"{abs(diff):.1f} балла ({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: bool(d.get("late_meal")) and bool(d.get("high_evening_activity")),
            outcome_key="sleep_duration",
            insight_type="late_meal_and_evening_workout_sleep",
            title="Поздний ужин и вечерняя тренировка вместе бьют по сну",
            unit="hours",
            min_abs_diff=0.4,
            period_days=period_days,
            with_note="Сон, когда вечером были и тренировка, и поздняя еда",
            without_note="Сон в остальные дни",
            description=lambda w, n, diff: (
                f"Когда поздняя тренировка совпадала с поздним приёмом пищи, сон был короче "
                f"на {abs(diff):.1f} ч ({w:.1f} против {n:.1f} ч)."
            ),
            min_each=2,
            base_confidence=0.6,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: bool(d.get("late_meal")) and bool(d.get("late_caffeine")),
            outcome_key="sleep_duration",
            insight_type="late_caffeine_and_late_meal_sleep",
            title="Поздний кофеин вместе с поздним ужином связан с худшим сном",
            unit="hours",
            min_abs_diff=0.4,
            period_days=period_days,
            with_note="Сон в дни с поздним кофеином и поздней едой",
            without_note="Сон в остальные дни",
            description=lambda w, n, diff: (
                f"Когда кофеин и еда были поздно в один день, сон был короче на "
                f"{abs(diff):.1f} ч ({w:.1f} против {n:.1f} ч)."
            ),
            base_confidence=0.6,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("sleep_duration") or 0) < 6.5,
            outcome_key="mood",
            insight_type="short_sleep_low_mood",
            title="Короткий сон связан с более низким настроением",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Настроение после ночи короче 6.5 ч",
            without_note="Настроение после более длинного сна",
            description=lambda w, n, diff: (
                f"После ночей короче 6.5 ч настроение было ниже на {abs(diff):.1f} балла "
                f"({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("sleep_duration") or 0) < 6.5,
            outcome_key="stress",
            insight_type="short_sleep_high_stress",
            title="Короткий сон связан с более высоким стрессом",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Стресс после короткого сна",
            without_note="Стресс после более длинного сна",
            description=lambda w, n, diff: (
                f"После ночей короче 6.5 ч стресс был выше на {abs(diff):.1f} балла "
                f"({w:.1f} против {n:.1f})."
            ),
            higher_is_better=False,
            require_positive=True,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("sleep_duration") or 0) >= 7.5,
            outcome_key="energy",
            insight_type="good_sleep_high_energy",
            title="Сон от 7.5 часов связан с более высокой энергией",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Энергия после сна ≥7.5 ч",
            without_note="Энергия после более короткого сна",
            description=lambda w, n, diff: (
                f"После ночей от 7.5 ч энергия была выше на {diff:.1f} балла "
                f"({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
            impact_when_factor_worse="positive",
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: bool(d.get("evening_light_activity"))
            and not bool(d.get("high_evening_activity")),
            outcome_key="sleep_duration",
            insight_type="evening_walk_sleep_positive",
            title="Лёгкая вечерняя активность связана с более длинным сном",
            unit="hours",
            min_abs_diff=0.35,
            period_days=period_days,
            with_note="Сон в дни с вечерней прогулкой или лёгкой нагрузкой",
            without_note="Сон в дни без лёгкой вечерней активности",
            description=lambda w, n, diff: (
                f"В дни с лёгкой вечерней активностью сон был длиннее на {diff:.1f} ч "
                f"({w:.1f} против {n:.1f} ч)."
            ),
            impact_when_factor_worse="positive",
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("hydration_ml") or 0) < 1600
            and int(d.get("activity_minutes") or 0) >= 40,
            outcome_key="energy",
            insight_type="low_hydration_high_activity_energy",
            title="Тренировка при недоборе воды связана с падением энергии",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Энергия в дни нагрузки при воде <1600 мл",
            without_note="Энергия в остальные дни",
            description=lambda w, n, diff: (
                f"В дни с нагрузкой и водой меньше 1600 мл энергия была ниже на "
                f"{abs(diff):.1f} балла ({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
            base_confidence=0.58,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("hydration_ml") or 0) >= 1800
            and float(d.get("sleep_duration") or 0) >= 7
            and int(d.get("steps") or 0) >= 7000,
            outcome_key="energy",
            insight_type="recovery_stack_high_energy",
            title="Сон + вода + шаги вместе связаны с лучшей энергией",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Энергия в дни, когда закрыты сон, вода и шаги",
            without_note="Энергия, когда хотя бы один столп не закрыт",
            description=lambda w, n, diff: (
                f"В дни, когда сон ≥7 ч, вода ≥1800 мл и шаги ≥7000, энергия была выше "
                f"на {diff:.1f} балла ({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
            impact_when_factor_worse="positive",
            base_confidence=0.58,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: bool(d.get("late_meal")),
            outcome_key="sleep_quality",
            insight_type="late_meal_sleep_quality_impact",
            title="Поздний ужин связан с худшим качеством сна",
            unit="score_1_10",
            min_abs_diff=0.7,
            period_days=period_days,
            with_note="Качество сна в дни с поздней едой",
            without_note="Качество сна без поздней еды",
            description=lambda w, n, diff: (
                f"В дни с поздним приёмом пищи качество сна было ниже на {abs(diff):.1f} "
                f"({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
        )
    )
    add(
        _maybe_insight(
            day_rows=day_rows,
            pred=lambda d: float(d.get("hydration_ml") or 0) < 1600,
            outcome_key="focus",
            insight_type="low_hydration_low_focus",
            title="Недобор воды связан с худшим фокусом",
            unit="score_1_10",
            min_abs_diff=0.8,
            period_days=period_days,
            with_note="Фокус в дни с водой <1600 мл",
            without_note="Фокус в дни с лучшей гидратацией",
            description=lambda w, n, diff: (
                f"В дни с водой меньше 1600 мл фокус был ниже на {abs(diff):.1f} балла "
                f"({w:.1f} против {n:.1f})."
            ),
            require_positive=True,
        )
    )

    return found


def format_patterns_for_llm(insights: list[dict[str, Any]]) -> str:
    if not insights:
        return ""
    lines = [
        "=== НАЙДЕННЫЕ ЗАКОНОМЕРНОСТИ (считает аналитика по дневнику) ===",
        "Это не общие факты, а сравнения дней этого пользователя. "
        "Опирайся на них, связывая сон, еду, воду и активность.",
    ]
    for item in insights[:10]:
        impact = "плюс" if item.get("impact") == "positive" else "риск"
        lines.append(
            f"- [{impact}] {item.get('title')}: {item.get('description')} "
            f"(уверенность {int(round(float(item.get('confidence') or 0) * 100))}%)"
        )
    return "\n".join(lines)
