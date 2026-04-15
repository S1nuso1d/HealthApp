from __future__ import annotations

from datetime import datetime, timedelta, timezone
from statistics import mean
from typing import Any

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.insight import Insight
from app.models.meal import MealRecord
from app.models.smart_reminder import SmartReminder
from app.models.smart_trigger import SmartTrigger
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.recommendations.recommendation_engine import RecommendationEngine


def safe_mean(values: list[float | int | None]) -> float:
    filtered = [float(v) for v in values if v is not None]
    if not filtered:
        return 0.0
    return mean(filtered)


def _active_trigger_exists(db: Session, user_id: int, trigger_type: str, title: str) -> bool:
    existing = (
        db.query(SmartTrigger)
        .filter(
            SmartTrigger.user_id == user_id,
            SmartTrigger.trigger_type == trigger_type,
            SmartTrigger.title == title,
            SmartTrigger.is_active == True,
            SmartTrigger.is_resolved == False,
        )
        .first()
    )
    return existing is not None


def _create_trigger_with_reminder(
    db: Session,
    user_id: int,
    trigger_type: str,
    category: str,
    title: str,
    description: str,
    severity: str,
    confidence: float,
    based_on: str | None,
    recommended_action: str | None,
    reminder_type: str,
    reminder_title: str,
    reminder_message: str,
    remind_at_label: str = "now",
) -> tuple[SmartTrigger, SmartReminder]:
    trigger = SmartTrigger(
        user_id=user_id,
        trigger_type=trigger_type,
        category=category,
        title=title,
        description=description,
        severity=severity,
        confidence=confidence,
        based_on=based_on,
        recommended_action=recommended_action,
        is_active=True,
        is_resolved=False,
    )
    db.add(trigger)
    db.flush()

    reminder = SmartReminder(
        user_id=user_id,
        trigger_id=trigger.id,
        reminder_type=reminder_type,
        title=reminder_title,
        message=reminder_message,
        status="new",
        is_active=True,
        remind_at_label=remind_at_label,
    )
    db.add(reminder)

    return trigger, reminder


def generate_smart_triggers_and_reminders(
    db: Session,
    user_id: int,
    period_days: int = 7,
) -> dict[str, Any]:
    now = datetime.now(timezone.utc)
    period_start = now - timedelta(days=period_days)

    triggers_created = 0
    reminders_created = 0

    sleeps = (
        db.query(SleepRecord)
        .filter(SleepRecord.user_id == user_id, SleepRecord.sleep_end >= period_start)
        .order_by(SleepRecord.sleep_end.desc())
        .all()
    )

    meals = (
        db.query(MealRecord)
        .filter(MealRecord.user_id == user_id, MealRecord.meal_time >= period_start)
        .order_by(MealRecord.meal_time.desc())
        .all()
    )

    hydration = (
        db.query(HydrationRecord)
        .filter(HydrationRecord.user_id == user_id, HydrationRecord.record_time >= period_start)
        .order_by(HydrationRecord.record_time.desc())
        .all()
    )

    activities = (
        db.query(ActivityRecord)
        .filter(ActivityRecord.user_id == user_id, ActivityRecord.start_time >= period_start)
        .order_by(ActivityRecord.start_time.desc())
        .all()
    )

    states = (
        db.query(UserState)
        .filter(UserState.user_id == user_id, UserState.record_time >= period_start)
        .order_by(UserState.record_time.desc())
        .all()
    )

    # ------------------------------------------------------------
    # RAW-PATTERN TRIGGERS
    # ------------------------------------------------------------

    # 1. 3 дня подряд недосып
    short_sleep_days = 0
    for s in sleeps[:3]:
        if (s.duration_hours or 0) < 6.5:
            short_sleep_days += 1

    if short_sleep_days >= 3:
        title = "Три дня подряд недосып"
        if not _active_trigger_exists(db, user_id, "sleep_streak_short", title):
            _create_trigger_with_reminder(
                db=db,
                user_id=user_id,
                trigger_type="sleep_streak_short",
                category="sleep",
                title=title,
                description="Система обнаружила три последних дня с короткой длительностью сна.",
                severity="high",
                confidence=0.93,
                based_on=f"Последние {short_sleep_days} дня сон был короче 6.5 часов.",
                recommended_action="Поставьте целью восстановить сон в ближайшие 2–3 дня.",
                reminder_type="sleep",
                reminder_title="Пора восстановить сон",
                reminder_message="Последние дни сна было мало. Сегодня лучше сделать вечер более спокойным и лечь раньше.",
                remind_at_label="evening",
            )
            triggers_created += 1
            reminders_created += 1

    # 2. 2 поздних кофе подряд
    recent_caffeine_days = 0
    meal_days_seen = set()
    for m in meals:
        day_key = m.meal_time.date().isoformat()
        if day_key in meal_days_seen:
            continue
        meal_days_seen.add(day_key)

        if (m.caffeine_mg or 0) >= 50 and (
            (m.minutes_before_sleep is not None and m.minutes_before_sleep < 360)
            or m.meal_time.hour >= 16
        ):
            recent_caffeine_days += 1

        if len(meal_days_seen) >= 2:
            break

    if recent_caffeine_days >= 2:
        title = "Два поздних кофе подряд"
        if not _active_trigger_exists(db, user_id, "late_caffeine_streak", title):
            _create_trigger_with_reminder(
                db=db,
                user_id=user_id,
                trigger_type="late_caffeine_streak",
                category="meals",
                title=title,
                description="Система заметила поздний кофеин в два последних дня.",
                severity="high",
                confidence=0.88,
                based_on="В последние два дня были напитки или приемы пищи с кофеином после 16:00 или близко ко сну.",
                recommended_action="Сделайте ближайшие 3–5 дней без кофеина после 15:00.",
                reminder_type="meal",
                reminder_title="Осторожно с вечерним кофеином",
                reminder_message="Поздний кофеин повторяется уже второй день подряд. Сегодня лучше остановиться на ранних напитках.",
                remind_at_label="today",
            )
            triggers_created += 1
            reminders_created += 1

    # 3. Мало воды в активные дни
    daily_hydration = {}
    daily_activity_minutes = {}

    for h in hydration:
        day_key = h.record_time.date().isoformat()
        daily_hydration.setdefault(day_key, 0.0)
        factor = float(h.hydration_factor) if h.hydration_factor is not None else 1.0
        daily_hydration[day_key] += float(h.amount_ml or 0) * factor

    for a in activities:
        day_key = a.start_time.date().isoformat()
        daily_activity_minutes.setdefault(day_key, 0)
        daily_activity_minutes[day_key] += int(a.duration_minutes or 0)

    low_hydration_active_days = 0
    for day_key, duration in daily_activity_minutes.items():
        hydration_amount = daily_hydration.get(day_key, 0.0)
        if duration >= 45 and hydration_amount < 1600:
            low_hydration_active_days += 1

    if low_hydration_active_days >= 2:
        title = "Мало воды в активные дни"
        if not _active_trigger_exists(db, user_id, "low_hydration_active_days", title):
            _create_trigger_with_reminder(
                db=db,
                user_id=user_id,
                trigger_type="low_hydration_active_days",
                category="hydration",
                title=title,
                description="В активные дни объем жидкости оказался ниже желаемого.",
                severity="medium",
                confidence=0.82,
                based_on=f"Обнаружено {low_hydration_active_days} активных дня с гидратацией ниже 1600 мл.",
                recommended_action="Добавляйте воду до и после активности.",
                reminder_type="hydration",
                reminder_title="Добавь воды в активный день",
                reminder_message="Сегодня при высокой активности важно не забыть про воду. Постарайся выпить больше в первой половине дня и после нагрузки.",
                remind_at_label="today",
            )
            triggers_created += 1
            reminders_created += 1

    # 4. Поздний ужин слишком близко ко сну
    recent_late_meals = [
        m for m in meals[:10]
        if bool(getattr(m, "is_late_meal", False)) or (
            m.minutes_before_sleep is not None and m.minutes_before_sleep < 120
        )
    ]

    if len(recent_late_meals) >= 2:
        title = "Ужин слишком близко ко сну"
        if not _active_trigger_exists(db, user_id, "late_meal_sleep_close", title):
            _create_trigger_with_reminder(
                db=db,
                user_id=user_id,
                trigger_type="late_meal_sleep_close",
                category="meals",
                title=title,
                description="За последние дни ужин часто оказывался слишком близко ко сну.",
                severity="medium",
                confidence=0.84,
                based_on=f"Обнаружено {len(recent_late_meals)} поздних приема пищи за недавний период.",
                recommended_action="Попробуйте закончить ужин за 2–3 часа до сна.",
                reminder_type="meal",
                reminder_title="Подумай о более раннем ужине",
                reminder_message="Поздний ужин повторяется уже не первый раз. Сегодня лучше завершить еду пораньше.",
                remind_at_label="evening",
            )
            triggers_created += 1
            reminders_created += 1

    # 5. Поздняя интенсивная тренировка
    recent_evening_high = [
        a for a in activities[:10]
        if bool(getattr(a, "is_evening_activity", False)) and (a.intensity or "").lower() == "high"
    ]

    if len(recent_evening_high) >= 2:
        title = "Поздняя интенсивная активность"
        if not _active_trigger_exists(db, user_id, "late_high_activity", title):
            _create_trigger_with_reminder(
                db=db,
                user_id=user_id,
                trigger_type="late_high_activity",
                category="activity",
                title=title,
                description="Интенсивные тренировки поздно вечером повторяются и могут мешать расслаблению.",
                severity="high",
                confidence=0.86,
                based_on=f"Найдено {len(recent_evening_high)} поздних интенсивных активностей.",
                recommended_action="Перенесите интенсивную нагрузку раньше или замените на легкую активность вечером.",
                reminder_type="activity",
                reminder_title="Вечером лучше снизить интенсивность",
                reminder_message="Поздние интенсивные тренировки могут мешать засыпанию. Если тренировка сегодня вечером, лучше сделать ее легче.",
                remind_at_label="evening",
            )
            triggers_created += 1
            reminders_created += 1

    # 6. Низкая энергия на фоне плохого сна
    avg_energy = safe_mean([s.energy for s in states if s.energy is not None])
    recent_sleep = sleeps[:3]
    recent_sleep_avg = safe_mean([s.duration_hours for s in recent_sleep if s.duration_hours is not None])

    if avg_energy > 0 and avg_energy < 5 and recent_sleep_avg < 6.5:
        title = "Низкая энергия на фоне плохого сна"
        if not _active_trigger_exists(db, user_id, "low_energy_poor_sleep", title):
            _create_trigger_with_reminder(
                db=db,
                user_id=user_id,
                trigger_type="low_energy_poor_sleep",
                category="state",
                title=title,
                description="Низкая энергия совпадает с ухудшением сна за последние дни.",
                severity="high",
                confidence=0.9,
                based_on=f"Средняя энергия {avg_energy:.1f}/10 при среднем сне {recent_sleep_avg:.1f} ч.",
                recommended_action="Сделайте сон главным приоритетом на ближайшие дни.",
                reminder_type="state",
                reminder_title="Энергия просела — начни со сна",
                reminder_message="Низкая энергия может быть следствием недосыпа. Сегодня лучше сократить вечернюю нагрузку и лечь раньше.",
                remind_at_label="evening",
            )
            triggers_created += 1
            reminders_created += 1

    # ------------------------------------------------------------
    # INSIGHT-BASED TRIGGERS
    # ------------------------------------------------------------
    insights = (
        db.query(Insight)
        .filter(Insight.user_id == user_id)
        .order_by(Insight.created_at.desc())
        .all()
    )

    for insight in insights:
        if insight.severity not in ("high", "medium"):
            continue

        trigger_type = f"insight_{insight.insight_type}"
        title = f"Инсайт: {insight.title}"

        if _active_trigger_exists(db, user_id, trigger_type, title):
            continue

        _create_trigger_with_reminder(
            db=db,
            user_id=user_id,
            trigger_type=trigger_type,
            category=insight.category,
            title=title,
            description=insight.description,
            severity=insight.severity,
            confidence=float(insight.confidence or 0.0),
            based_on=f"Insight type: {insight.insight_type}",
            recommended_action="Открой рекомендации и обрати внимание на этот паттерн.",
            reminder_type=insight.category,
            reminder_title=f"Обрати внимание: {insight.title}",
            reminder_message=insight.description,
            remind_at_label="today",
        )
        triggers_created += 1
        reminders_created += 1

    # ------------------------------------------------------------
    # RECOMMENDATION-BASED REMINDERS
    # ------------------------------------------------------------
    recommendations = RecommendationEngine.generate_recommendations(db=db, user_id=user_id)

    for rec in recommendations[:5]:
        reminder_trigger_type = f"recommendation_{rec.related_insight_type or rec.title}"
        reminder_title = f"Рекомендация: {rec.title}"

        if _active_trigger_exists(db, user_id, reminder_trigger_type, reminder_title):
            continue

        trigger, reminder = _create_trigger_with_reminder(
            db=db,
            user_id=user_id,
            trigger_type=reminder_trigger_type,
            category=rec.category,
            title=reminder_title,
            description=rec.description,
            severity="high" if rec.priority == "high" else "medium" if rec.priority == "medium" else "low",
            confidence=float(rec.confidence or 0.0),
            based_on=rec.related_insight_title,
            recommended_action=rec.action,
            reminder_type=rec.category,
            reminder_title=rec.title,
            reminder_message=rec.action or rec.description,
            remind_at_label="today",
        )
        triggers_created += 1
        reminders_created += 1

    db.commit()

    return {
        "triggers_created": triggers_created,
        "reminders_created": reminders_created,
    }