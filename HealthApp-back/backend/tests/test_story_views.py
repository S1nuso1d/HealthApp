import uuid
from datetime import datetime, timedelta, timezone

from app.models.social import FeedStory, Friendship
from app.models.user import User


def _register(client, suffix: str) -> tuple[dict[str, str], int]:
    from app.services.registration_email import LAST_SENT_CODES

    email = f"story_{suffix}@example.com"
    password = "testpassword123"
    resp = client.post("/auth/register/start", json={"email": email, "password": password})
    assert resp.status_code == 200, resp.text
    code = LAST_SENT_CODES[email.lower()]
    resp2 = client.post(
        "/auth/register/complete",
        json={"email": email, "password": password, "code": code},
    )
    assert resp2.status_code == 200, resp2.text
    return {"Authorization": f"Bearer {resp2.json()['access_token']}"}, email


def _make_friends(db_session, user_a: int, user_b: int) -> None:
    db_session.add(
        Friendship(
            requester_id=user_a,
            addressee_id=user_b,
            status="accepted",
        )
    )
    db_session.commit()


def test_story_view_count_excludes_owner_and_expires(client, db_session):
    suffix = uuid.uuid4().hex[:8]
    headers_a, email_a = _register(client, f"a_{suffix}")
    headers_b, email_b = _register(client, f"b_{suffix}")
    user_a = db_session.query(User).filter(User.email == email_a).first().id
    user_b = db_session.query(User).filter(User.email == email_b).first().id
    _make_friends(db_session, user_a, user_b)

    create = client.post(
        "/social/stories",
        json={"media_url": "https://example.com/story.jpg"},
        headers=headers_a,
    )
    assert create.status_code == 200, create.text
    story_id = create.json()["id"]

    view_b = client.post(f"/social/stories/{story_id}/view", headers=headers_b)
    assert view_b.status_code == 200, view_b.text
    assert view_b.json()["view_count"] == 1

    view_a = client.post(f"/social/stories/{story_id}/view", headers=headers_a)
    assert view_a.status_code == 200, view_a.text
    assert view_a.json()["view_count"] == 1

    view_b_again = client.post(f"/social/stories/{story_id}/view", headers=headers_b)
    assert view_b_again.status_code == 200
    assert view_b_again.json()["view_count"] == 1

    stories = client.get("/social/stories", headers=headers_a)
    assert stories.status_code == 200, stories.text
    own_group = next(s for s in stories.json()["stories"] if s["author"]["is_self"])
    own_item = next(i for i in own_group["items"] if i["id"] == story_id)
    assert own_item["view_count"] == 1

    story = db_session.query(FeedStory).filter(FeedStory.id == story_id).first()
    story.expires_at = datetime.now(timezone.utc) - timedelta(minutes=1)
    db_session.commit()

    expired = client.get("/social/stories", headers=headers_a)
    assert expired.status_code == 200
    assert all(s["author"]["user_id"] != user_a for s in expired.json()["stories"])
