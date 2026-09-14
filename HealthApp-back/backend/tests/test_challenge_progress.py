"""Прогресс челленджей должен считаться из реальных записей дневника.

Регрессия, из-за которой появились эти тесты: `ChallengeParticipant.progress`
никто не записывал, и таблица лидеров всегда показывала нули.
"""

from datetime import datetime, timedelta, timezone

import pytest

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.social import Challenge, ChallengeParticipant
from app.services import challenge_progress_service


@pytest.fixture
def user_id(client, auth_headers, db_session) -> int:
    from app.models.user import User

    email = auth_headers["Authorization"]
    assert email  # заголовок получен, пользователь создан
    user = db_session.query(User).order_by(User.id.desc()).first()
    return user.id


def _challenge(db_session, creator_id: int, challenge_type: str, target: int) -> Challenge:
    now = datetime.now(timezone.utc)
    challenge = Challenge(
        title=f"Тест {challenge_type}",
        description=None,
        challenge_type=challenge_type,
        target_value=target,
        is_group=True,
        creator_id=creator_id,
        start_date=now - timedelta(days=3),
        end_date=now + timedelta(days=3),
    )
    db_session.add(challenge)
    db_session.commit()
    db_session.refresh(challenge)
    return challenge


def test_steps_challenge_progress_sums_activity(client, auth_headers, db_session, user_id):
    challenge = _challenge(db_session, user_id, "steps", 10000)
    db_session.add(ChallengeParticipant(challenge_id=challenge.id, user_id=user_id))

    now = datetime.now(timezone.utc)
    for days_ago, steps in ((1, 3000), (2, 4500)):
        start = now - timedelta(days=days_ago)
        db_session.add(
            ActivityRecord(
                user_id=user_id,
                activity_type="walk",
                start_time=start,
                end_time=start + timedelta(minutes=40),
                duration_minutes=40,
                steps=steps,
            )
        )
    db_session.commit()

    challenge_progress_service.refresh_challenge(db_session, challenge)

    participant = (
        db_session.query(ChallengeParticipant)
        .filter(ChallengeParticipant.challenge_id == challenge.id)
        .one()
    )
    assert participant.progress == 7500


def test_water_challenge_progress_sums_hydration(client, auth_headers, db_session, user_id):
    challenge = _challenge(db_session, user_id, "water", 5000)
    db_session.add(ChallengeParticipant(challenge_id=challenge.id, user_id=user_id))

    now = datetime.now(timezone.utc)
    for amount in (500.0, 750.0, 250.0):
        db_session.add(
            HydrationRecord(
                user_id=user_id,
                amount_ml=amount,
                drink_type="water",
                record_time=now - timedelta(hours=2),
            )
        )
    db_session.commit()

    challenge_progress_service.refresh_challenge(db_session, challenge)

    participant = (
        db_session.query(ChallengeParticipant)
        .filter(ChallengeParticipant.challenge_id == challenge.id)
        .one()
    )
    assert participant.progress == 1500


def test_records_outside_window_are_ignored(client, auth_headers, db_session, user_id):
    challenge = _challenge(db_session, user_id, "steps", 10000)
    db_session.add(ChallengeParticipant(challenge_id=challenge.id, user_id=user_id))

    # Запись за месяц до старта челленджа в зачёт идти не должна.
    old = datetime.now(timezone.utc) - timedelta(days=30)
    db_session.add(
        ActivityRecord(
            user_id=user_id,
            activity_type="walk",
            start_time=old,
            end_time=old + timedelta(minutes=30),
            duration_minutes=30,
            steps=9999,
        )
    )
    db_session.commit()

    challenge_progress_service.refresh_challenge(db_session, challenge)

    participant = (
        db_session.query(ChallengeParticipant)
        .filter(ChallengeParticipant.challenge_id == challenge.id)
        .one()
    )
    assert participant.progress == 0


def test_leaderboard_returns_computed_progress(client, auth_headers, db_session, user_id):
    challenge = _challenge(db_session, user_id, "steps", 10000)
    db_session.add(ChallengeParticipant(challenge_id=challenge.id, user_id=user_id))
    now = datetime.now(timezone.utc)
    db_session.add(
        ActivityRecord(
            user_id=user_id,
            activity_type="run",
            start_time=now - timedelta(hours=5),
            end_time=now - timedelta(hours=4),
            duration_minutes=60,
            steps=6200,
        )
    )
    db_session.commit()

    resp = client.get(f"/social/challenges/{challenge.id}/leaderboard", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["challenge_type"] == "steps"
    assert body["target_value"] == 10000
    assert body["entries"][0]["progress"] == 6200
    assert body["entries"][0]["is_me"] is True


def test_create_challenge_rejects_type_without_progress_support(client, auth_headers):
    """Тип, который нечем считать, не должен создаваться: иначе прогресс навсегда 0."""
    resp = client.post(
        "/social/challenges",
        headers=auth_headers,
        json={
            "title": "Странный челлендж",
            "description": None,
            "challenge_type": "telepathy",
            "target_value": 10,
            "is_group": False,
            "duration_days": 7,
        },
    )
    assert resp.status_code == 422


def test_join_counts_progress_accumulated_before_joining(client, auth_headers, db_session, user_id):
    """Присоединившийся позже получает уже накопленное за окно, а не ноль."""
    challenge = _challenge(db_session, user_id, "water", 5000)
    now = datetime.now(timezone.utc)
    db_session.add(
        HydrationRecord(
            user_id=user_id,
            amount_ml=900.0,
            drink_type="water",
            record_time=now - timedelta(days=1),
        )
    )
    db_session.commit()

    resp = client.post(f"/social/challenges/{challenge.id}/join", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    assert resp.json()["my_progress"] == 900
