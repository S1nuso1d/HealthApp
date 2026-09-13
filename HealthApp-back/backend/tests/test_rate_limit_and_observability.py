"""Ограничение частоты запросов и сквозной идентификатор запроса."""

import uuid

from app.core.config import settings
from app.core.observability import REQUEST_ID_HEADER
from app.core.rate_limit import _parse_rule, limiter


def test_parse_rule_reads_limit_and_window():
    assert _parse_rule("10/60", (1, 1)) == (10, 60)
    assert _parse_rule("garbage", (5, 30)) == (5, 30)
    assert _parse_rule("0/60", (5, 30)) == (5, 30)


def test_sliding_window_blocks_after_limit():
    limiter.reset()
    key = f"test:{uuid.uuid4().hex}"
    assert limiter.check(key, limit=2, window_seconds=60) == 0
    assert limiter.check(key, limit=2, window_seconds=60) == 0
    retry_after = limiter.check(key, limit=2, window_seconds=60)
    assert retry_after > 0


def test_limiter_separates_different_clients():
    limiter.reset()
    assert limiter.check("scope:1.1.1.1", limit=1, window_seconds=60) == 0
    assert limiter.check("scope:2.2.2.2", limit=1, window_seconds=60) == 0
    assert limiter.check("scope:1.1.1.1", limit=1, window_seconds=60) > 0


def test_login_is_rate_limited(client, rate_limited):
    """Без лимита /auth/login открыт для перебора пароля."""
    limit, _window = _parse_rule(settings.RATE_LIMIT_AUTH, (10, 60))
    statuses = [
        client.post(
            "/auth/login",
            data={"username": "victim@example.com", "password": "wrong"},
        ).status_code
        for _ in range(limit + 3)
    ]
    # Первые попытки отклоняются как неверный пароль, дальше срабатывает лимит
    assert statuses[0] == 401
    assert statuses[-1] == 429


def test_rate_limit_can_be_disabled(client):
    """Автоиспользуемая фикстура выключает лимит — тесты не должны его ловить."""
    statuses = {
        client.post(
            "/auth/login",
            data={"username": "nobody@example.com", "password": "wrong"},
        ).status_code
        for _ in range(30)
    }
    assert statuses == {401}


def test_rate_limit_response_has_retry_after(client, rate_limited):
    last = None
    for _ in range(40):
        last = client.post(
            "/auth/login",
            data={"username": "victim2@example.com", "password": "wrong"},
        )
        if last.status_code == 429:
            break
    assert last is not None and last.status_code == 429
    assert "retry-after" in {k.lower() for k in last.headers}


def test_response_carries_request_id(client):
    resp = client.get("/")
    assert resp.status_code == 200
    assert resp.headers.get(REQUEST_ID_HEADER)


def test_incoming_request_id_is_preserved(client):
    """Позволяет склеить логи мобильного клиента и сервера по одному идентификатору."""
    provided = "client-trace-1234"
    resp = client.get("/", headers={REQUEST_ID_HEADER: provided})
    assert resp.headers.get(REQUEST_ID_HEADER) == provided


def test_error_response_includes_request_id(client):
    resp = client.get("/profile/me")
    assert resp.status_code == 401
    body = resp.json()
    assert "detail" in body
    assert body.get("request_id")


def test_healthz_reports_database(client):
    resp = client.get("/healthz")
    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok"
    assert body["database"] == "ok"
    assert body["version"]
