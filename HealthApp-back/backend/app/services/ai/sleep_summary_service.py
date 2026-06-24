"""Генерация саммари по звукам сна: LLM + русскоязычный fallback."""

from __future__ import annotations

import re
from collections import Counter
from datetime import datetime, timezone

from app.core.config import settings
from app.llm.llm_client import LLMClient, LLMClientError
from app.schemas.ai import SleepSoundRecord, SleepSummaryResponse

_SLEEP_SUMMARY_SYSTEM = """
Ты помощник приложения HealthApp по анализу звуков во сне.

Правила:
1. Отвечай ТОЛЬКО на русском языке (кириллица). Никакого китайского, английского и других языков.
2. Не ставь диагнозы и не назначай лечение.
3. До 3 коротких предложений, заботливый тон, на «вы».
4. Опирайся только на переданный список звуков — не выдумывай факты.
""".strip()

_CJK_RE = re.compile(r"[\u4e00-\u9fff\u3400-\u4dbf\uf900-\ufaff]")


def _looks_non_russian(text: str) -> bool:
    stripped = (text or "").strip()
    if not stripped:
        return True
    if _CJK_RE.search(stripped):
        return True
    cyrillic = len(re.findall(r"[А-Яа-яЁё]", stripped))
    latin = len(re.findall(r"[A-Za-z]", stripped))
    return cyrillic == 0 and latin > 20


def build_sleep_summary_prompt(sounds: list[SleepSoundRecord]) -> str:
    lines = [
        "Проанализируй список звуков, зафиксированных во время сна пользователя этой ночью.",
        "Сделай короткое резюме (до 3 предложений) на русском языке.",
        "Пример стиля: «Сегодня ночью чаще всего фиксировался храп, в основном ближе к утру. Сон был немного беспокойным.»",
        "",
        "Список звуков:",
    ]
    for sound in sounds:
        loudness = f", громкость {sound.peakRms:.2f}" if sound.peakRms is not None else ""
        lines.append(f"- {sound.time}: {sound.label}{loudness}")
    lines.append("")
    lines.append("Ответь строго на русском языке.")
    return "\n".join(lines)


def build_fallback_sleep_summary(sounds: list[SleepSoundRecord]) -> str:
    if not sounds:
        return "Звуков за эту ночь не зафиксировано. Сон был тихим."

    label_counts = Counter(s.label for s in sounds if s.label)
    total = len(sounds)
    if not label_counts:
        return f"За ночь зафиксировано {total} звуковых эпизодов. Подробная расшифровка недоступна."

    parts = [f"«{label}» — {count}" for label, count in label_counts.most_common()]
    joined = ", ".join(parts)
    if total == 1:
        return f"За ночь зафиксирован 1 звуковой эпизод: {joined}."
    return (
        f"За ночь зафиксировано {total} звуковых эпизодов: {joined}. "
        "Это автоматическая сводка по датчику (LLM сейчас недоступна)."
    )


def generate_sleep_summary(sounds: list[SleepSoundRecord]) -> SleepSummaryResponse:
    generated_at = datetime.now(timezone.utc)

    if not sounds:
        return SleepSummaryResponse(
            summary="Звуков за эту ночь не зафиксировано. Сон был тихим.",
            generated_at=generated_at,
        )

    if not settings.LLM_ENABLED:
        return SleepSummaryResponse(
            summary=build_fallback_sleep_summary(sounds),
            generated_at=generated_at,
        )

    client = LLMClient()
    prompt = build_sleep_summary_prompt(sounds)

    try:
        raw = client.generate(prompt=prompt, system_prompt=_SLEEP_SUMMARY_SYSTEM).strip()
        if _looks_non_russian(raw):
            raw = build_fallback_sleep_summary(sounds)
        return SleepSummaryResponse(summary=raw, generated_at=generated_at)
    except LLMClientError:
        if settings.AI_FALLBACK_ENABLED:
            return SleepSummaryResponse(
                summary=build_fallback_sleep_summary(sounds),
                generated_at=generated_at,
            )
        raise
