from __future__ import annotations

import re


def sanitize_llm_markdown(text: str) -> str:
    """Убирает markdown-разметку из ответов чата, если модель её проигнорировала."""
    raw = (text or "").strip()
    if not raw:
        return ""

    raw = re.sub(r"```\w*\n?", "", raw)
    raw = re.sub(r"^#{1,6}\s+(.+)$", r"\1", raw, flags=re.MULTILINE)
    raw = re.sub(r"^#+\s*$", "", raw, flags=re.MULTILINE)
    raw = re.sub(r"\*\*(.+?)\*\*", r"\1", raw)
    raw = re.sub(r"__(.+?)__", r"\1", raw)
    raw = re.sub(r"(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)", r"\1", raw)
    raw = re.sub(r"\*{2,}", "", raw)
    raw = re.sub(r"\n{3,}", "\n\n", raw)
    return raw.strip()
