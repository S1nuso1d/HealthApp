from __future__ import annotations

from datetime import datetime

from zoneinfo import ZoneInfo

from app.core.config import settings


def get_local_now() -> datetime:
    return datetime.now(ZoneInfo(settings.APP_TIMEZONE))


def describe_day_period(hour: int) -> tuple[str, str]:
    if 5 <= hour < 12:
        return "утро", "подходит для завтрака, гидратации и лёгкой активности"
    if 12 <= hour < 17:
        return "день", "время обеда, движения и поддержания водного баланса"
    if 17 <= hour < 22:
        return "вечер", "ужин, спокойная активность и подготовка ко сну"
    return "ночь", "пора отдыхать и ложиться спать; избегайте яркого света и кофеина"


def build_time_context_block(sleep_target_hours: float | None = None) -> str:
    now = get_local_now()
    period, guidance = describe_day_period(now.hour)
    lines = [
        "=== ТЕКУЩЕЕ ВРЕМЯ (локальное) ===",
        f"Сейчас: {now.strftime('%d.%m.%Y %H:%M')} ({period})",
        f"Контекст времени суток: {guidance}.",
        "Учитывайте время суток в советах: утром — вода и завтрак, днём — активность, "
        "вечером — спокойный режим, ночью — сон и отдых.",
    ]
    target = sleep_target_hours or 8.0
    if now.hour >= 22 or now.hour < 5:
        lines.append(
            f"Целевой сон: {target:.1f} ч. Если пользователь ещё не лёг — мягко предложите "
            "завершить дела и лечь спать."
        )
    elif now.hour >= 21:
        lines.append(
            "Приближается время сна — предложите завершить активные дела и начать вечерний ритуал."
        )
    return "\n".join(lines)


def time_aware_hint(sleep_target_hours: float | None = None) -> str | None:
    now = get_local_now()
    target = sleep_target_hours or 8.0
    if now.hour >= 23 or now.hour < 5:
        return (
            f"Уже поздно ({now.strftime('%H:%M')}) — лучше лечь спать, "
            f"чтобы набрать целевые {target:.0f} ч сна."
        )
    if now.hour >= 22:
        return "Пора готовиться ко сну: приглушите свет и отложите экран."
    if now.hour >= 21:
        return "Вечер — хорошее время завершить дела и начать спокойный ритуал перед сном."
    if 5 <= now.hour < 10:
        return "Доброе утро — начните день со стакана воды и лёгкого завтрака."
    if 12 <= now.hour < 14:
        return "Обеденное время — не пропустите полноценный приём пищи и воду."
    return None
