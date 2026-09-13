"""Тесты жизненного цикла токенов: типы, ротация, отзыв."""

import uuid

import pytest

from app.core.security import (
    ACCESS_TOKEN_TYPE,
    REFRESH_TOKEN_TYPE,
    TokenError,
    create_access_token,
    create_refresh_token,
    decode_token,
    user_id_from_token,
)


def register(client) -> tuple[str, str, str]:
    """Возвращает (email, access_token, refresh_token)."""
    from app.services.registration_email import LAST_SENT_CODES

    email = f"tok_{uuid.uuid4().hex[:10]}@example.com"
    password = "testpassword123"
    start = client.post(
        "/auth/register/start", json={"email": email, "password": password}
    )
    assert start.status_code == 200, start.text
    code = LAST_SENT_CODES[email.lower()]
    done = client.post(
        "/auth/register/complete",
        json={"email": email, "password": password, "code": code},
    )
    assert done.status_code == 200, done.text
    body = done.json()
    return email, body["access_token"], body["refresh_token"]


def test_tokens_carry_type_claim():
    access = create_access_token({"sub": "1"})
    refresh = create_refresh_token({"sub": "1"})
    assert decode_token(access, ACCESS_TOKEN_TYPE)["type"] == ACCESS_TOKEN_TYPE
    assert decode_token(refresh, REFRESH_TOKEN_TYPE)["type"] == REFRESH_TOKEN_TYPE


def test_refresh_token_rejected_where_access_expected():
    """Иначе долгоживущий refresh работал бы как access-токен 30 дней."""
    refresh = create_refresh_token({"sub": "1"})
    with pytest.raises(TokenError):
        user_id_from_token(refresh, ACCESS_TOKEN_TYPE)


def test_access_token_rejected_where_refresh_expected():
    access = create_access_token({"sub": "1"})
    with pytest.raises(TokenError):
        decode_token(access, REFRESH_TOKEN_TYPE)


def test_refresh_token_cannot_be_used_as_bearer(client):
    _, _, refresh = register(client)
    resp = client.get("/profile/me", headers={"Authorization": f"Bearer {refresh}"})
    assert resp.status_code == 401


def test_access_token_rejected_on_refresh_endpoint(client):
    _, access, _ = register(client)
    resp = client.post("/auth/refresh", json={"refresh_token": access})
    assert resp.status_code == 401


def test_refresh_rotates_and_old_token_stops_working(client):
    _, _, refresh = register(client)

    first = client.post("/auth/refresh", json={"refresh_token": refresh})
    assert first.status_code == 200, first.text
    rotated = first.json()["refresh_token"]
    assert rotated != refresh

    # Новый работает
    second = client.post("/auth/refresh", json={"refresh_token": rotated})
    assert second.status_code == 200, second.text


def test_refresh_reuse_revokes_all_sessions(client):
    """Повторное предъявление потраченного refresh — признак кражи: рубим все сессии."""
    _, _, refresh = register(client)

    first = client.post("/auth/refresh", json={"refresh_token": refresh})
    assert first.status_code == 200
    rotated = first.json()["refresh_token"]

    reused = client.post("/auth/refresh", json={"refresh_token": refresh})
    assert reused.status_code == 401

    # Токен, выданный законно, тоже отозван — злоумышленник не сможет им пользоваться
    after = client.post("/auth/refresh", json={"refresh_token": rotated})
    assert after.status_code == 401


def test_logout_revokes_refresh_tokens(client):
    _, access, refresh = register(client)
    headers = {"Authorization": f"Bearer {access}"}

    assert client.post("/auth/logout", headers=headers).status_code == 200
    assert client.post("/auth/refresh", json={"refresh_token": refresh}).status_code == 401


def test_password_change_revokes_refresh_tokens(client):
    _, access, refresh = register(client)
    headers = {"Authorization": f"Bearer {access}"}

    changed = client.post(
        "/auth/change-password",
        json={"current_password": "testpassword123", "new_password": "brandnewpass456"},
        headers=headers,
    )
    assert changed.status_code == 200, changed.text
    assert client.post("/auth/refresh", json={"refresh_token": refresh}).status_code == 401


def test_garbage_token_is_rejected(client):
    resp = client.get("/profile/me", headers={"Authorization": "Bearer not.a.jwt"})
    assert resp.status_code == 401


def test_token_signed_with_other_secret_is_rejected(client):
    from jose import jwt

    forged = jwt.encode(
        {"sub": "1", "type": ACCESS_TOKEN_TYPE, "exp": 9999999999},
        "totally-different-secret",
        algorithm="HS256",
    )
    resp = client.get("/profile/me", headers={"Authorization": f"Bearer {forged}"})
    assert resp.status_code == 401
