"""Автоматические метки времени приёма пищи относительно сна."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone


def _aware(dt: datetime) -> datetime:
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt


def infer_is_late_meal(meal_time: datetime, minutes_before_sleep: int | None = None) -> bool:
    if minutes_before_sleep is not None and minutes_before_sleep < 180:
        return True
    local = _aware(meal_time)
    return local.hour > 21 or (local.hour == 21 and local.minute >= 0)


def infer_minutes_before_sleep(
    meal_time: datetime,
    next_sleep_start: datetime | None,
) -> int | None:
    if next_sleep_start is None:
        return None
    meal = _aware(meal_time)
    sleep = _aware(next_sleep_start)
    if sleep <= meal:
        return None
    return int((sleep - meal).total_seconds() // 60)


def find_next_sleep_start(
    meal_time: datetime,
    sleep_starts: list[datetime],
) -> datetime | None:
    meal = _aware(meal_time)
    candidates = [_aware(s) for s in sleep_starts if _aware(s) > meal]
    if not candidates:
        return None
    return min(candidates)


def enrich_meal_timing(
    meal_time: datetime,
    sleep_starts: list[datetime],
    is_late_meal: bool | None = None,
    minutes_before_sleep: int | None = None,
) -> tuple[bool, int | None]:
    next_sleep = find_next_sleep_start(meal_time, sleep_starts)
    mins = minutes_before_sleep
    if mins is None and next_sleep is not None:
        mins = infer_minutes_before_sleep(meal_time, next_sleep)
    late = is_late_meal
    if late is None:
        late = infer_is_late_meal(meal_time, mins)
    return bool(late), mins
