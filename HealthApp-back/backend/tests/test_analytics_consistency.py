"""Р‘Р°Р»Р» РїРёС‚Р°РЅРёСЏ РЅРµ РґРѕР»Р¶РµРЅ Р·Р°РІРёСЃРµС‚СЊ РѕС‚ С‚РѕРіРѕ, СЃ РєР°РєРѕРіРѕ СЌРєСЂР°РЅР° РµРіРѕ СЃРјРѕС‚СЂСЏС‚.

Р Р°РЅСЊС€Рµ `/analytics/overview` СЃС‡РёС‚Р°Р» РїРёС‚Р°РЅРёРµ РїРѕ СЃСЂРµРґРЅРµРјСѓ РєРѕС„РµРёРЅСѓ, Р° РґР°С€Р±РѕСЂРґ вЂ”
РїРѕ РєР°Р»РѕСЂРёСЏРј РѕС‚РЅРѕСЃРёС‚РµР»СЊРЅРѕ С†РµР»Рё: РѕРґРёРЅ Рё С‚РѕС‚ Р¶Рµ РїРѕРєР°Р·Р°С‚РµР»СЊ РїРѕРєР°Р·С‹РІР°Р» СЂР°Р·РЅС‹Рµ С‡РёСЃР»Р°.
"""

from datetime import date, timedelta

from app.models.daily_health_summary import DailyHealthSummary
from app.models.profile import UserProfile
from app.services.health_score_service import compute_period_average_scores


def _set_calorie_target(db_session, user_id: int, target: int) -> None:
    """РџСЂРѕС„РёР»СЊ РјРѕР¶РµС‚ Р±С‹С‚СЊ СѓР¶Рµ СЃРѕР·РґР°РЅ СЂРµРіРёСЃС‚СЂР°С†РёРµР№ вЂ” РѕР±РЅРѕРІР»СЏРµРј, Р° РЅРµ РІСЃС‚Р°РІР»СЏРµРј."""
    profile = db_session.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    if profile is None:
        profile = UserProfile(user_id=user_id)
        db_session.add(profile)
    profile.target_daily_calories = target
    db_session.commit()


def _seed_week(db_session, user_id: int, *, calories: float, caffeine: float) -> None:
    today = date.today()
    for offset in range(7):
        db_session.add(
            DailyHealthSummary(
                user_id=user_id,
                summary_date=today - timedelta(days=offset),
                total_sleep_hours=8.0,
                total_water_ml=2500,
                total_calories=calories,
                total_caffeine_mg=caffeine,
                total_steps=10000,
                total_active_minutes=45,
                workouts_count=1,
                total_state_score=80.0,
            )
        )
    db_session.commit()


def test_nutrition_score_follows_calories_not_caffeine(db_session, test_user):
    """Р”РІР° РЅР°Р±РѕСЂР° РґР°РЅРЅС‹С… СЂР°Р·Р»РёС‡Р°СЋС‚СЃСЏ С‚РѕР»СЊРєРѕ РєРѕС„РµРёРЅРѕРј вЂ” Р±Р°Р»Р» РїРёС‚Р°РЅРёСЏ РјРµРЅСЏС‚СЊСЃСЏ РЅРµ РґРѕР»Р¶РµРЅ."""
    _set_calorie_target(db_session, test_user.id, 2000)
    _seed_week(db_session, test_user.id, calories=2000.0, caffeine=0.0)

    low_caffeine = compute_period_average_scores(
        db=db_session,
        user_id=test_user.id,
        start_date=date.today() - timedelta(days=6),
        end_date=date.today(),
    )

    for summary in db_session.query(DailyHealthSummary).filter(
        DailyHealthSummary.user_id == test_user.id
    ):
        summary.total_caffeine_mg = 500.0
    db_session.commit()

    high_caffeine = compute_period_average_scores(
        db=db_session,
        user_id=test_user.id,
        start_date=date.today() - timedelta(days=6),
        end_date=date.today(),
    )

    assert low_caffeine["nutrition_score"] == high_caffeine["nutrition_score"]
    # РљР°Р»РѕСЂРёРё С‚РѕС‡РЅРѕ РІ С†РµР»СЊ вЂ” Р±Р°Р»Р» РјР°РєСЃРёРјР°Р»СЊРЅС‹Р№.
    assert low_caffeine["nutrition_score"] == 100


def test_period_scores_respect_profile_targets(db_session, test_user):
    """Р¦РµР»СЊ РїРѕ РєР°Р»РѕСЂРёСЏРј РёР· РїСЂРѕС„РёР»СЏ РґРѕР»Р¶РЅР° СѓС‡РёС‚С‹РІР°С‚СЊСЃСЏ, Р° РЅРµ РїРѕРґСЃС‚Р°РІР»СЏС‚СЊСЃСЏ РїРѕ СѓРјРѕР»С‡Р°РЅРёСЋ."""
    _set_calorie_target(db_session, test_user.id, 3000)
    _seed_week(db_session, test_user.id, calories=1500.0, caffeine=0.0)

    scores = compute_period_average_scores(
        db=db_session,
        user_id=test_user.id,
        start_date=date.today() - timedelta(days=6),
        end_date=date.today(),
    )

    # 1500 РёР· 3000 вЂ” РїРѕР»РѕРІРёРЅР° С†РµР»Рё, Р±Р°Р»Р» Р·Р°РјРµС‚РЅРѕ РЅРёР¶Рµ РјР°РєСЃРёРјСѓРјР°.
    assert scores["nutrition_score"] < 70


def test_state_score_uses_daily_summaries(db_session, test_user):
    _seed_week(db_session, test_user.id, calories=2200.0, caffeine=50.0)

    scores = compute_period_average_scores(
        db=db_session,
        user_id=test_user.id,
        start_date=date.today() - timedelta(days=6),
        end_date=date.today(),
    )

    assert scores["state_score"] == 80


def test_overview_endpoint_matches_service(client, auth_headers, db_session):
    from app.models.user import User

    user = db_session.query(User).order_by(User.id.desc()).first()
    _set_calorie_target(db_session, user.id, 2000)
    _seed_week(db_session, user.id, calories=2000.0, caffeine=400.0)

    resp = client.get("/analytics/overview?days=7", headers=auth_headers)
    assert resp.status_code == 200, resp.text
    summary = resp.json()["summary"]

    expected = compute_period_average_scores(
        db=db_session,
        user_id=user.id,
        start_date=date.today() - timedelta(days=6),
        end_date=date.today(),
    )
    assert summary["nutrition_score"] == expected["nutrition_score"]
    assert summary["health_score"] == expected["health_score"]
