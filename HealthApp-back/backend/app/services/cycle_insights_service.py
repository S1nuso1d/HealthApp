"""Связь фаз цикла с данными дневника.

Прогноз цикла (следующие месячные, овуляция, фертильное окно) уже считается
на клиенте в `CycleCalculator`. Чего не было — связи фазы с фактическими
данными: как меняются сон, энергия и активность по фазам. Это единственное,
что нельзя посчитать на клиенте, потому что нужны дневные сводки за месяцы.
"""

from __future__ import annotations

from datetime import date, timedelta

from sqlalchemy.orm import Session

from app.models.daily_health_summary import DailyHealthSummary
from app.models.health import CycleEntry

DEFAULT_CYCLE_LENGTH = 28
DEFAULT_PERIOD_LENGTH = 5

PHASES = ("menstrual", "follicular", "ovulation", "luteal")

PHASE_TITLES = {
    "menstrual": "Менструальная",
    "follicular": "Фолликулярная",
    "ovulation": "Овуляция",
    "luteal": "Лютеиновая",
}


def _sorted_starts(entries: list[CycleEntry]) -> list[date]:
    return sorted({e.start_date for e in entries if e.start_date})


def _average_cycle_length(starts: list[date]) -> int:
    """Средняя длина цикла по интервалам между началами месячных."""
    if len(starts) < 2:
        return DEFAULT_CYCLE_LENGTH
    gaps = [
        (later - earlier).days
        for earlier, later in zip(starts[:-1], starts[1:], strict=True)
        # Интервалы вне физиологического диапазона — это пропущенная запись,
        # а не настоящий цикл: усреднять их нельзя.
        if 18 <= (later - earlier).days <= 45
    ]
    if not gaps:
        return DEFAULT_CYCLE_LENGTH
    return round(sum(gaps) / len(gaps))


def _average_period_length(entries: list[CycleEntry]) -> int:
    lengths = [
        (e.end_date - e.start_date).days + 1
        for e in entries
        if e.start_date and e.end_date and e.end_date >= e.start_date
    ]
    if not lengths:
        return DEFAULT_PERIOD_LENGTH
    return max(1, round(sum(lengths) / len(lengths)))


def _phase_for_day(cycle_day: int, cycle_length: int, period_length: int) -> str:
    """Фаза по номеру дня цикла (1 — первый день месячных)."""
    if cycle_day <= period_length:
        return "menstrual"
    # Овуляция привязана к концу цикла, а не к его началу: лютеиновая фаза
    # стабильна (~14 дней), тогда как фолликулярная растягивается.
    ovulation_day = max(period_length + 1, cycle_length - 14)
    if cycle_day < ovulation_day - 1:
        return "follicular"
    if cycle_day <= ovulation_day + 1:
        return "ovulation"
    return "luteal"


def _phase_by_date(
    starts: list[date],
    cycle_length: int,
    period_length: int,
    day: date,
) -> str | None:
    """К какой фазе относится календарный день."""
    previous_start = None
    for start in starts:
        if start <= day:
            previous_start = start
        else:
            break
    if previous_start is None:
        return None

    cycle_day = (day - previous_start).days + 1
    # Слишком далеко от последней записи — цикл, скорее всего, не отслеживался.
    if cycle_day > cycle_length + 10:
        return None
    return _phase_for_day(cycle_day, cycle_length, period_length)


def _average(values: list[float]) -> float | None:
    clean = [v for v in values if v is not None]
    if not clean:
        return None
    return round(sum(clean) / len(clean), 2)


def _observations(stats: dict[str, dict]) -> list[dict]:
    """Человекочитаемые наблюдения: чем фаза отличается от остальных."""
    observations: list[dict] = []

    metrics = (
        ("sleep_hours", "сон", "ч", 0.4),
        ("state_score", "самочувствие", "балл", 6.0),
        ("steps", "шаги", "шагов", 900.0),
    )

    for key, human_name, unit, threshold in metrics:
        values = {
            phase: data[key]
            for phase, data in stats.items()
            if data.get(key) is not None and data.get("days", 0) >= 3
        }
        if len(values) < 2:
            continue

        overall = sum(values.values()) / len(values)
        for phase, value in values.items():
            delta = value - overall
            if abs(delta) < threshold:
                continue
            direction = "выше" if delta > 0 else "ниже"
            observations.append(
                {
                    "phase": phase,
                    "phase_title": PHASE_TITLES[phase],
                    "metric": key,
                    "delta": round(delta, 2),
                    "impact": "positive" if delta > 0 else "negative",
                    "text": (
                        f"{PHASE_TITLES[phase]} фаза: {human_name} "
                        f"{direction} обычного на {abs(round(delta, 2))} {unit}."
                    ),
                }
            )

    observations.sort(key=lambda o: abs(o["delta"]), reverse=True)
    return observations


def build_cycle_insights(db: Session, user_id: int, months: int = 6) -> dict:
    entries = (
        db.query(CycleEntry)
        .filter(CycleEntry.user_id == user_id)
        .order_by(CycleEntry.start_date.asc())
        .all()
    )
    starts = _sorted_starts(entries)

    cycle_length = _average_cycle_length(starts)
    period_length = _average_period_length(entries)

    if not starts:
        return {
            "average_cycle_length": cycle_length,
            "average_period_length": period_length,
            "tracked_cycles": 0,
            "phase_stats": [],
            "observations": [],
            "has_enough_data": False,
            "message": "Добавь хотя бы два цикла, чтобы связать фазы с самочувствием.",
        }

    window_start = date.today() - timedelta(days=months * 31)
    summaries = (
        db.query(DailyHealthSummary)
        .filter(
            DailyHealthSummary.user_id == user_id,
            DailyHealthSummary.summary_date >= max(window_start, starts[0]),
        )
        .all()
    )

    buckets: dict[str, dict[str, list[float]]] = {
        phase: {"sleep_hours": [], "state_score": [], "steps": [], "water_ml": []}
        for phase in PHASES
    }
    for summary in summaries:
        phase = _phase_by_date(starts, cycle_length, period_length, summary.summary_date)
        if phase is None:
            continue
        bucket = buckets[phase]
        if summary.total_sleep_hours:
            bucket["sleep_hours"].append(float(summary.total_sleep_hours))
        if summary.total_state_score is not None:
            bucket["state_score"].append(float(summary.total_state_score))
        if summary.total_steps:
            bucket["steps"].append(float(summary.total_steps))
        if summary.total_water_ml:
            bucket["water_ml"].append(float(summary.total_water_ml))

    stats = {
        phase: {
            "days": max(len(values) for values in bucket.values()) if bucket else 0,
            "sleep_hours": _average(bucket["sleep_hours"]),
            "state_score": _average(bucket["state_score"]),
            "steps": _average(bucket["steps"]),
            "water_ml": _average(bucket["water_ml"]),
        }
        for phase, bucket in buckets.items()
    }

    observations = _observations(stats)
    tracked_cycles = max(0, len(starts) - 1)

    return {
        "average_cycle_length": cycle_length,
        "average_period_length": period_length,
        "tracked_cycles": tracked_cycles,
        "phase_stats": [
            {"phase": phase, "phase_title": PHASE_TITLES[phase], **stats[phase]}
            for phase in PHASES
        ],
        "observations": observations,
        "has_enough_data": bool(observations),
        "message": (
            None
            if observations
            else "Данных пока мало: нужно несколько циклов с записями сна и самочувствия."
        ),
    }
