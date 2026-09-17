"""Эндпоинты, добавленные вместе с новыми экранами приложения."""

from datetime import date, datetime, timedelta, timezone

from app.models.daily_health_summary import DailyHealthSummary
from app.models.health import CycleEntry
from app.models.profile import UserProfile
from app.models.user import User


def _current_user(db_session) -> User:
    return db_session.query(User).order_by(User.id.desc()).first()


def _profile(db_session, user_id: int, **fields) -> UserProfile:
    """Профиль создаётся при регистрации, поэтому его правим, а не вставляем заново."""
    profile = db_session.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    if profile is None:
        profile = UserProfile(user_id=user_id)
        db_session.add(profile)
    for key, value in fields.items():
        setattr(profile, key, value)
    db_session.commit()
    return profile


# --------------------------------------------------------------------------
# Состояние: поле wellbeing раньше существовало в модели, но не в API
# --------------------------------------------------------------------------


def test_wellbeing_round_trips_through_api(client, auth_headers):
    resp = client.post(
        "/states/",
        headers=auth_headers,
        json={
            "mood": 7,
            "energy": 6,
            "stress": 3,
            "focus": 8,
            "wellbeing": 9,
            "record_time": datetime.now(timezone.utc).isoformat(),
            "notes": None,
        },
    )
    assert resp.status_code == 200, resp.text
    assert resp.json()["wellbeing"] == 9

    listing = client.get("/states/", headers=auth_headers)
    assert listing.status_code == 200
    assert listing.json()[0]["wellbeing"] == 9


# --------------------------------------------------------------------------
# Факторы влияния
# --------------------------------------------------------------------------


def test_influence_factors_reports_insufficient_data(client, auth_headers):
    resp = client.get("/analytics/influence-factors?days=14", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["has_enough_data"] is False
    assert body["factors"] == []
    assert body["message"]


# --------------------------------------------------------------------------
# Сравнение запусков аналитики
# --------------------------------------------------------------------------


def test_compare_requires_two_runs(client, auth_headers):
    resp = client.get("/analytics/compare", headers=auth_headers)
    assert resp.status_code == 409
    assert "два пересчёта" in resp.json()["detail"]


def test_compare_two_runs(client, auth_headers):
    first = client.post("/analytics/rebuild?days=7", headers=auth_headers)
    assert first.status_code == 200, first.text
    second = client.post("/analytics/rebuild?days=7", headers=auth_headers)
    assert second.status_code == 200, second.text

    resp = client.get("/analytics/compare", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["summary"]["current_run_id"] == second.json()["id"]
    assert body["summary"]["previous_run_id"] == first.json()["id"]
    metrics = {d["metric"] for d in body["score_deltas"]}
    assert "nutrition_score" in metrics
    assert body["progress_insights"]


# --------------------------------------------------------------------------
# Сохранённые рекомендации
# --------------------------------------------------------------------------


def test_saved_recommendations_listing_and_status(client, auth_headers, db_session):
    from app.models.saved_recommendation import SavedRecommendation

    user = _current_user(db_session)
    db_session.add(
        SavedRecommendation(
            user_id=user.id,
            category="sleep",
            title="Ложиться раньше",
            description="Сдвиньте отбой на 30 минут",
            priority="high",
            status="new",
        )
    )
    db_session.commit()

    listing = client.get("/analytics/saved-recommendations", headers=auth_headers)
    assert listing.status_code == 200, listing.text
    items = listing.json()
    assert len(items) == 1
    item_id = items[0]["id"]

    patched = client.patch(
        f"/analytics/saved-recommendations/{item_id}?status=read",
        headers=auth_headers,
    )
    assert patched.status_code == 200
    assert patched.json()["status"] == "read"

    filtered = client.get(
        "/analytics/saved-recommendations?status=new", headers=auth_headers
    )
    assert filtered.json() == []


def test_saved_recommendation_rejects_bad_status(client, auth_headers):
    resp = client.patch(
        "/analytics/saved-recommendations/1?status=whatever", headers=auth_headers
    )
    assert resp.status_code == 422


# --------------------------------------------------------------------------
# Экспорт
# --------------------------------------------------------------------------


def test_export_report_contains_period_and_scores(client, auth_headers, db_session):
    user = _current_user(db_session)
    db_session.add(
        DailyHealthSummary(
            user_id=user.id,
            summary_date=date.today(),
            total_sleep_hours=7.5,
            total_water_ml=2100,
            total_calories=1950.0,
            total_caffeine_mg=80.0,
            total_steps=8400,
            total_active_minutes=35,
            workouts_count=1,
            total_state_score=72.0,
        )
    )
    db_session.commit()

    resp = client.get("/export/report?days=30", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["meta"]["period_days"] == 30
    assert "nutrition_score" in body["scores"]
    assert len(body["daily"]) == 1
    assert body["daily"][0]["steps"] == 8400


def test_export_csv_is_semicolon_separated_with_bom(client, auth_headers, db_session):
    user = _current_user(db_session)
    db_session.add(
        DailyHealthSummary(
            user_id=user.id,
            summary_date=date.today(),
            total_sleep_hours=8.0,
            total_water_ml=2500,
            total_calories=2000.0,
            total_caffeine_mg=0.0,
            total_steps=10000,
            total_active_minutes=40,
            workouts_count=1,
            total_state_score=80.0,
        )
    )
    db_session.commit()

    resp = client.get("/export/csv?days=7", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    text = resp.content.decode("utf-8")
    assert text.startswith("\ufeff")
    header = text.splitlines()[0].lstrip("\ufeff")
    assert header.split(";")[0] == "date"
    assert "steps" in header


# --------------------------------------------------------------------------
# Наблюдения по фазам цикла
# --------------------------------------------------------------------------


def test_cycle_insights_requires_female_profile(client, auth_headers, db_session):
    user = _current_user(db_session)
    _profile(db_session, user.id, sex="male")

    resp = client.get("/cycle/insights", headers=auth_headers)
    assert resp.status_code == 403


def test_cycle_insights_returns_phase_stats(client, auth_headers, db_session):
    user = _current_user(db_session)
    _profile(db_session, user.id, sex="female")

    # Два цикла по 28 дней, чтобы средняя длина считалась по фактическим данным.
    first_start = date.today() - timedelta(days=56)
    for offset in (0, 28):
        start = first_start + timedelta(days=offset)
        db_session.add(
            CycleEntry(
                user_id=user.id,
                start_date=start,
                end_date=start + timedelta(days=4),
            )
        )
    db_session.commit()

    resp = client.get("/cycle/insights?months=6", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["average_cycle_length"] == 28
    assert body["average_period_length"] == 5
    assert body["tracked_cycles"] == 1
    phases = [row["phase"] for row in body["phase_stats"]]
    assert phases == ["menstrual", "follicular", "ovulation", "luteal"]
    assert body["current_phase"] in {"menstrual", "follicular", "ovulation", "luteal"}
    assert "recovery_tip" in body
    assert "activity_minutes" in body["phase_stats"][0]
