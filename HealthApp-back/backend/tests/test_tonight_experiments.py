"""Риск сна вечером, недельный эксперимент, объяснимость факторов, цикл."""

from datetime import date, datetime, timedelta, timezone

from app.models.activity import ActivityRecord
from app.models.daily_health_summary import DailyHealthSummary
from app.models.habit_experiment import HabitExperiment
from app.models.health import CycleEntry
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.services.ai.time_context import get_local_now
from app.services.influence_factors_service import build_influence_factors

from tests.test_new_endpoints import _current_user, _profile


def test_tonight_risk_is_low_without_levers(client, auth_headers):
    resp = client.get("/analytics/tonight-risk", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["risk_level"] == "low"
    assert body["levers"] == []
    assert body["suggested_experiment"] is None


def test_tonight_risk_flags_late_meal_and_caffeine(client, auth_headers, db_session):
    user = _current_user(db_session)
    now = get_local_now()
    db_session.add(
        MealRecord(
            user_id=user.id,
            meal_type="dinner",
            name="Паста",
            calories=900,
            caffeine_mg=80,
            meal_time=now.replace(hour=21, minute=10, second=0, microsecond=0),
            is_late_meal=True,
        )
    )
    db_session.commit()

    resp = client.get("/analytics/tonight-risk", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["risk_level"] in {"medium", "high"}
    ids = {item["id"] for item in body["levers"]}
    assert "late_meal_sleep_impact" in ids
    assert "late_caffeine_sleep_impact" in ids
    assert body["suggested_experiment"]["factor_id"] in ids
    assert body["primary_action"]


def test_habit_experiment_start_and_cancel(client, auth_headers):
    started = client.post(
        "/analytics/experiments?factor_id=late_caffeine_sleep_impact",
        headers=auth_headers,
    )
    assert started.status_code == 200, started.text
    body = started.json()
    assert body["status"] == "active"
    assert body["days_total"] == 7
    assert body["title"]

    listing = client.get("/analytics/experiments", headers=auth_headers)
    assert listing.status_code == 200, listing.text
    listed = listing.json()
    assert listed["active"]["id"] == body["id"]
    assert listed["items"]

    cancelled = client.post(
        f"/analytics/experiments/{body['id']}/cancel",
        headers=auth_headers,
    )
    assert cancelled.status_code == 200
    assert cancelled.json()["status"] == "cancelled"

    empty = client.get("/analytics/experiments", headers=auth_headers)
    assert empty.json()["active"] is None


def test_habit_experiment_completes_after_week(client, auth_headers, db_session):
    user = _current_user(db_session)
    today = get_local_now().date()
    started = today - timedelta(days=8)
    ended = today - timedelta(days=1)

    for offset in range(8, 1, -1):
        day = datetime.combine(today - timedelta(days=offset), datetime.min.time()).replace(
            tzinfo=timezone.utc
        )
        db_session.add(
            SleepRecord(
                user_id=user.id,
                sleep_start=day.replace(hour=23) - timedelta(days=1),
                sleep_end=day.replace(hour=7),
                duration_hours=7.6,
            )
        )
    db_session.add(
        HabitExperiment(
            user_id=user.id,
            factor_id="late_caffeine_sleep_impact",
            title="Без кофеина после 15:00",
            action="7 дней без кофеина после 15:00",
            metric="sleep_hours",
            status="active",
            started_on=started,
            ends_on=ended,
            baseline_value=6.4,
        )
    )
    db_session.commit()

    listing = client.get("/analytics/experiments", headers=auth_headers)
    assert listing.status_code == 200, listing.text
    active = listing.json()["active"]
    assert active is not None
    assert active["status"] == "completed"
    assert active["result_value"] is not None
    assert "Сработало" in active["result_summary"] or "Почти" in active["result_summary"]


def test_influence_proof_line_includes_day_counts(db_session, test_user):
    now = datetime.now(timezone.utc)
    user_id = test_user.id
    for i in range(8):
        day = now - timedelta(days=i)
        is_bad = i % 2 == 0
        sleep_hours = 6.1 if is_bad else 7.4
        sleep_end = day.replace(hour=7, minute=0)
        db_session.add(
            SleepRecord(
                user_id=user_id,
                sleep_start=sleep_end - timedelta(hours=sleep_hours),
                sleep_end=sleep_end,
                duration_hours=sleep_hours,
                quality_score=4 if is_bad else 8,
            )
        )
        db_session.add(
            MealRecord(
                user_id=user_id,
                meal_type="dinner",
                name="Ужин",
                calories=800,
                protein_g=30,
                meal_time=day.replace(hour=22 if is_bad else 18, minute=0),
                is_late_meal=is_bad,
                minutes_before_sleep=60 if is_bad else 240,
                caffeine_mg=80 if is_bad else 0,
            )
        )
        db_session.add(
            ActivityRecord(
                user_id=user_id,
                activity_type="gym",
                start_time=day.replace(hour=21 if is_bad else 12, minute=0),
                end_time=day.replace(hour=22 if is_bad else 13, minute=0),
                duration_minutes=50,
                steps=9000 if not is_bad else 3000,
                intensity="high" if is_bad else "medium",
                is_evening_activity=is_bad,
                minutes_before_sleep=90 if is_bad else 500,
            )
        )
        db_session.add(
            HydrationRecord(
                user_id=user_id,
                amount_ml=1200 if is_bad else 2200,
                drink_type="water",
                record_time=day.replace(hour=15, minute=0),
            )
        )
        db_session.add(
            UserState(
                user_id=user_id,
                energy=4 if is_bad else 8,
                mood=4 if is_bad else 8,
                stress=8 if is_bad else 3,
                focus=4 if is_bad else 8,
                wellbeing=4 if is_bad else 8,
                record_time=day.replace(hour=12, minute=0),
            )
        )
    db_session.commit()

    payload = build_influence_factors(db_session, user_id, period_days=14)
    assert payload["factors"]
    with_proof = next(
        (item for item in payload["factors"] if item.get("comparison", {}).get("proof_line")),
        None,
    )
    assert with_proof is not None
    comparison = with_proof["comparison"]
    assert comparison["with_days"]
    assert comparison["without_days"]
    assert "днях с фактором" in comparison["proof_line"]
    assert with_proof["suggested_action"]


def test_cycle_insights_recovery_tip_uses_phase_activity(client, auth_headers, db_session):
    user = _current_user(db_session)
    _profile(db_session, user.id, sex="female")

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

    # Менструальные дни — короткий сон и мало минут, фолликулярные — наоборот.
    for day_offset in range(56):
        day = first_start + timedelta(days=day_offset)
        cycle_day = (day_offset % 28) + 1
        menstrual = cycle_day <= 5
        db_session.add(
            DailyHealthSummary(
                user_id=user.id,
                summary_date=day,
                total_sleep_hours=6.0 if menstrual else 7.6,
                total_state_score=55 if menstrual else 78,
                total_steps=4000 if menstrual else 9500,
                total_active_minutes=12 if menstrual else 48,
                workouts_count=0 if menstrual else 1,
                total_water_ml=1800,
            )
        )
    db_session.commit()

    resp = client.get("/cycle/insights?months=6", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["current_phase"] in {"menstrual", "follicular", "ovulation", "luteal"}
    assert body["current_phase_title"]
    assert body["recovery_tip"]
    minutes = {row["phase"]: row["activity_minutes"] for row in body["phase_stats"]}
    assert minutes["menstrual"] is not None
    assert minutes["follicular"] is not None
    assert minutes["menstrual"] < minutes["follicular"]


def test_circadian_usual_bedtime_and_meal_curve(client, auth_headers, db_session):
    user = _current_user(db_session)
    now = get_local_now()
    for i in range(8):
        night = now - timedelta(days=i)
        start = night.replace(hour=22, minute=10, second=0, microsecond=0)
        late_dinner = i % 2 == 0
        db_session.add(
            SleepRecord(
                user_id=user.id,
                sleep_start=start,
                sleep_end=start + timedelta(hours=6.1 if late_dinner else 7.6),
                duration_hours=6.1 if late_dinner else 7.6,
            )
        )
        db_session.add(
            MealRecord(
                user_id=user.id,
                meal_type="dinner",
                name="Ужин",
                calories=700,
                meal_time=start - timedelta(hours=1.2 if late_dinner else 4.5),
                is_late_meal=late_dinner,
            )
        )
    db_session.commit()

    resp = client.get("/analytics/circadian", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    body = resp.json()
    assert body["sample_nights"] >= 4
    assert body["usual_bedtime"] is not None
    assert body["usual_bedtime_hour"] in {21, 22, 23}
    assert body["hours_before_sleep"]
    buckets = {row["bucket"] for row in body["hours_before_sleep"]}
    assert buckets & {"0_2", "4_plus"}


def test_workout_near_bedtime_warning_text():
    from app.services.circadian_service import workout_near_bedtime_warning

    start = get_local_now().replace(hour=22, minute=0, second=0, microsecond=0)
    usual = 22 * 60 + 10
    warning = workout_near_bedtime_warning(start, usual, intensity="high")
    assert warning
    assert "22:10" in warning
    assert workout_near_bedtime_warning(start, usual, intensity="low") is None
