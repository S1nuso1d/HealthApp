from __future__ import annotations

import json
import re


def parse_llm_json(text: str) -> dict:
    """Извлекает JSON-объект из ответа LLM (в т.ч. обёрнутого в ```json)."""
    raw = (text or "").strip()
    if not raw:
        raise ValueError("Пустой ответ LLM")

    fenced = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", raw, re.IGNORECASE)
    if fenced:
        raw = fenced.group(1).strip()

    start = raw.find("{")
    end = raw.rfind("}")
    if start >= 0 and end > start:
        raw = raw[start : end + 1]

    data = json.loads(raw)
    if not isinstance(data, dict):
        raise ValueError("Ожидался JSON-объект")
    return data
