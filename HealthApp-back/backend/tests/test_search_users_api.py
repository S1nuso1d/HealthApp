import uuid

from app.models.profile import UserProfile
from app.models.user import User


def _register_with_profile(client, email: str, password: str, profile: dict) -> str:
    from app.services.registration_email import LAST_SENT_CODES

    resp = client.post(
        "/auth/register/start",
        json={"email": email, "password": password},
    )
    assert resp.status_code == 200, resp.text
    code = LAST_SENT_CODES[email.lower()]
    resp2 = client.post(
        "/auth/register/complete",
        json={
            "email": email,
            "password": password,
            "code": code,
            "profile": profile,
        },
    )
    assert resp2.status_code == 200, resp2.text
    return resp2.json()["access_token"]


def test_search_users_finds_cyrillic_names(client, db_session):
    suffix = uuid.uuid4().hex[:8]
    email_a = f"search_a_{suffix}@example.com"
    email_b = f"search_b_{suffix}@example.com"
    password = "testpassword123"

    token_a = _register_with_profile(
        client,
        email_a,
        password,
        {"first_name": "Данил", "last_name": "Татаринцев", "nickname": "danil_search"},
    )
    _register_with_profile(
        client,
        email_b,
        password,
        {"first_name": "Варвара", "last_name": "Обухова", "nickname": "varvara_search"},
    )

    headers = {"Authorization": f"Bearer {token_a}"}

    for query in ("обухова", "варвара", "варвара обухова", "varvara_search"):
        resp = client.get("/social/users/search", params={"q": query}, headers=headers)
        assert resp.status_code == 200, resp.text
        ids = {u["user_id"] for u in resp.json()["users"]}
        user_b = db_session.query(User).filter(User.email == email_b).first()
        assert user_b is not None
        assert user_b.id in ids, f"query {query!r} did not find user B"

    resp_danil = client.get("/social/users/search", params={"q": "данил"}, headers=headers)
    assert resp_danil.status_code == 200
    ids_danil = {u["user_id"] for u in resp_danil.json()["users"]}
    user_a = db_session.query(User).filter(User.email == email_a).first()
    assert user_a is not None
    assert user_a.id not in ids_danil


def test_search_users_finds_email_when_profile_empty(client, db_session):
    suffix = uuid.uuid4().hex[:8]
    email_a = f"finder_{suffix}@example.com"
    email_b = f"tatarincev.finder_{suffix}@example.com"
    password = "testpassword123"

    token_a = _register_with_profile(client, email_a, password, {"nickname": "finder"})
    _register_with_profile(client, email_b, password, {"first_name": "Иван", "last_name": "Иванов"})

    user_b = db_session.query(User).filter(User.email == email_b).first()
    user_b.profile = db_session.query(UserProfile).filter(UserProfile.user_id == user_b.id).first()
    user_b.profile.first_name = None
    user_b.profile.last_name = None
    db_session.commit()

    headers = {"Authorization": f"Bearer {token_a}"}
    resp = client.get(
        "/social/users/search",
        params={"q": "tatarincev.finder"},
        headers=headers,
    )
    assert resp.status_code == 200, resp.text
    ids = {u["user_id"] for u in resp.json()["users"]}
    assert user_b.id in ids
