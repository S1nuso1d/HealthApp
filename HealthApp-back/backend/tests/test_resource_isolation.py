"""Проверка изоляции данных между пользователями и защиты загрузок.

Тесты закрывают класс уязвимостей IDOR: доступ к чужой записи по угаданному id.
"""

import uuid

import pytest

PNG_BYTES = (
    b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"
    b"\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89"
    b"\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4"
    b"\x00\x00\x00\x00IEND\xaeB`\x82"
)


def make_user(client) -> dict[str, str]:
    from app.services.registration_email import LAST_SENT_CODES

    email = f"iso_{uuid.uuid4().hex[:10]}@example.com"
    password = "testpassword123"
    start = client.post("/auth/register/start", json={"email": email, "password": password})
    assert start.status_code == 200, start.text
    code = LAST_SENT_CODES[email.lower()]
    done = client.post(
        "/auth/register/complete",
        json={"email": email, "password": password, "code": code},
    )
    assert done.status_code == 200, done.text
    return {"Authorization": f"Bearer {done.json()['access_token']}"}


@pytest.fixture
def two_users(client):
    return make_user(client), make_user(client)


def test_meal_of_another_user_is_not_readable(client, two_users):
    owner, intruder = two_users
    created = client.post(
        "/meal/",
        json={
            "meal_type": "breakfast",
            "name": "Личный завтрак",
            "calories": 400.0,
            "meal_time": "2026-05-14T08:30:00",
            "source": "manual",
        },
        headers=owner,
    )
    assert created.status_code == 200, created.text
    meal_id = created.json()["id"]

    assert client.get(f"/meal/{meal_id}", headers=owner).status_code == 200
    assert client.get(f"/meal/{meal_id}", headers=intruder).status_code == 404


def test_meal_of_another_user_cannot_be_modified_or_deleted(client, two_users):
    owner, intruder = two_users
    created = client.post(
        "/meal/",
        json={
            "meal_type": "lunch",
            "name": "Обед",
            "calories": 600.0,
            "meal_time": "2026-05-14T13:00:00",
            "source": "manual",
        },
        headers=owner,
    )
    meal_id = created.json()["id"]

    updated = client.put(
        f"/meal/{meal_id}",
        json={
            "meal_type": "lunch",
            "name": "Взломано",
            "calories": 1.0,
            "meal_time": "2026-05-14T13:00:00",
            "source": "manual",
        },
        headers=intruder,
    )
    assert updated.status_code == 404
    assert client.delete(f"/meal/{meal_id}", headers=intruder).status_code == 404

    # Запись владельца не изменилась
    still_there = client.get(f"/meal/{meal_id}", headers=owner)
    assert still_there.status_code == 200
    assert still_there.json()["name"] == "Обед"


def test_hydration_and_sleep_are_isolated(client, two_users):
    owner, intruder = two_users

    water = client.post(
        "/hydration/",
        json={"amount_ml": 500, "record_time": "2026-05-14T10:00:00"},
        headers=owner,
    )
    assert water.status_code == 200, water.text
    assert client.get(f"/hydration/{water.json()['id']}", headers=intruder).status_code == 404

    sleep = client.post(
        "/sleep/",
        json={
            "sleep_start": "2026-05-13T23:00:00",
            "sleep_end": "2026-05-14T07:00:00",
            "quality": 4,
        },
        headers=owner,
    )
    assert sleep.status_code == 200, sleep.text
    assert client.get(f"/sleep/{sleep.json()['id']}", headers=intruder).status_code == 404


def test_history_shows_only_own_records(client, two_users):
    owner, intruder = two_users
    client.post(
        "/meal/",
        json={
            "meal_type": "dinner",
            "name": "Только мой ужин",
            "calories": 500.0,
            "meal_time": "2026-05-14T19:00:00",
            "source": "manual",
        },
        headers=owner,
    )
    other_history = client.get("/meal/history", headers=intruder)
    assert other_history.status_code == 200
    assert other_history.json() == []


def test_feed_media_requires_authentication(client):
    """Раньше эндпоинт был публичным: знание имени файла давало доступ к чужому фото."""
    headers = make_user(client)
    uploaded = client.post(
        "/social/feed/media",
        files={"file": ("photo.png", PNG_BYTES, "image/png")},
        headers=headers,
    )
    assert uploaded.status_code == 200, uploaded.text
    url = uploaded.json()["url"]

    assert client.get(url, headers=headers).status_code == 200
    assert client.get(url).status_code == 401


def test_feed_media_rejects_path_traversal(client):
    headers = make_user(client)
    for bad in ("../../healthapp.db", "..%2F..%2Fhealthapp.db", "not-a-uuid.png"):
        resp = client.get(f"/social/feed/media/{bad}", headers=headers)
        assert resp.status_code in (400, 404), f"{bad} -> {resp.status_code}"


def test_avatar_upload_rejects_non_image(client):
    headers = make_user(client)
    resp = client.post(
        "/profile/me/avatar",
        files={"file": ("payload.png", b"<?php echo 1; ?>", "image/png")},
        headers=headers,
    )
    assert resp.status_code == 400


def test_avatar_upload_rejects_oversized_file(client):
    from app.core.config import settings

    headers = make_user(client)
    oversized = PNG_BYTES + b"\x00" * (settings.AVATAR_MAX_BYTES + 1)
    resp = client.post(
        "/profile/me/avatar",
        files={"file": ("big.png", oversized, "image/png")},
        headers=headers,
    )
    assert resp.status_code == 400


def test_protected_endpoints_require_token(client):
    for method, path in (
        ("get", "/profile/me"),
        ("get", "/meal/history"),
        ("get", "/sleep/history"),
        ("get", "/hydration/history"),
        ("get", "/social/feed"),
    ):
        resp = getattr(client, method)(path)
        assert resp.status_code == 401, f"{path} -> {resp.status_code}"
