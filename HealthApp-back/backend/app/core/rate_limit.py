"""Ограничение частоты запросов для чувствительных эндпоинтов.

Счётчики живут в памяти процесса, поэтому лимит действует на один воркер uvicorn.
Для одного инстанса (текущий сценарий развёртывания) этого достаточно и не требует
Redis. При масштабировании на несколько воркеров счётчики нужно вынести в Redis.
"""

import threading
import time
from collections import defaultdict, deque

from fastapi import HTTPException, Request, status

from app.core.config import settings


def _parse_rule(rule: str, fallback: tuple[int, int]) -> tuple[int, int]:
    """"10/60" -> (10 запросов, 60 секунд)."""
    try:
        limit_raw, window_raw = rule.split("/", 1)
        limit, window = int(limit_raw), int(window_raw)
        if limit > 0 and window > 0:
            return limit, window
    except (ValueError, AttributeError):
        pass
    return fallback


class SlidingWindowLimiter:
    def __init__(self) -> None:
        self._hits: dict[str, deque[float]] = defaultdict(deque)
        self._lock = threading.Lock()

    def check(self, key: str, limit: int, window_seconds: int) -> int:
        """Возвращает 0, если запрос разрешён, иначе число секунд до разблокировки."""
        now = time.monotonic()
        cutoff = now - window_seconds
        with self._lock:
            hits = self._hits[key]
            while hits and hits[0] < cutoff:
                hits.popleft()
            if len(hits) >= limit:
                return max(1, int(hits[0] + window_seconds - now) + 1)
            hits.append(now)
            if not hits:
                self._hits.pop(key, None)
            return 0

    def reset(self) -> None:
        with self._lock:
            self._hits.clear()


limiter = SlidingWindowLimiter()


def _client_key(request: Request, scope: str) -> str:
    forwarded = request.headers.get("x-forwarded-for", "")
    client_ip = forwarded.split(",")[0].strip() if forwarded else ""
    if not client_ip and request.client:
        client_ip = request.client.host
    return f"{scope}:{client_ip or 'unknown'}"


def rate_limit(scope: str, rule: str, fallback: tuple[int, int] = (10, 60)):
    """Зависимость FastAPI: ограничивает запросы по IP в рамках одного scope."""
    limit, window = _parse_rule(rule, fallback)

    def dependency(request: Request) -> None:
        if not settings.RATE_LIMIT_ENABLED:
            return
        retry_after = limiter.check(_client_key(request, scope), limit, window)
        if retry_after:
            raise HTTPException(
                status_code=status.HTTP_429_TOO_MANY_REQUESTS,
                detail="Слишком много запросов. Попробуйте позже.",
                headers={"Retry-After": str(retry_after)},
            )

    return dependency


auth_rate_limit = rate_limit("auth", settings.RATE_LIMIT_AUTH, (10, 60))
upload_rate_limit = rate_limit("upload", settings.RATE_LIMIT_UPLOAD, (30, 60))
ai_rate_limit = rate_limit("ai", settings.RATE_LIMIT_AI, (20, 60))
