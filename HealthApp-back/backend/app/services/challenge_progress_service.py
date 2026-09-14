"""Расчёт прогресса участников челленджей по реальным записям дневника.

`ChallengeParticipant.progress` раньше всегда оставался нулём: его никто не
записывал, и таблица лидеров показывала нули. Здесь прогресс считается как
накопленное значение метрики за окно челленджа (от `start_date` до `end_date`).
"""

from __future__ import annotations

from datetime import datetime, timezone

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.sleep import SleepRecord
from app.models.social import Challenge, ChallengeParticipant

# Тип челленджа -> (модель, колонка времени, агрегат по одной записи).
# Агрегат — SQL-выражение, чтобы суммировать на стороне БД одним запросом
# сразу по всем участникам, а не тянуть записи в память.
_METRICS = {
    "steps": (ActivityRecord, ActivityRecord.start_time, func.sum(ActivityRecord.steps)),
    "calories": (
        ActivityRecord,
        ActivityRecord.start_time,
        func.sum(ActivityRecord.calories_burned),
    ),
    "active_minutes": (
        ActivityRecord,
        ActivityRecord.start_time,
        func.sum(ActivityRecord.duration_minutes),
    ),
    "workouts": (ActivityRecord, ActivityRecord.start_time, func.count(ActivityRecord.id)),
    "water": (
        HydrationRecord,
        HydrationRecord.record_time,
        func.sum(HydrationRecord.amount_ml),
    ),
    "sleep": (SleepRecord, SleepRecord.sleep_start, func.sum(SleepRecord.duration_hours)),
    "calories_eaten": (MealRecord, MealRecord.meal_time, func.sum(MealRecord.calories)),
}

SUPPORTED_TYPES = frozenset(_METRICS)


def _as_utc(value: datetime | None) -> datetime | None:
    """SQLite отдаёт naive datetime — приводим к UTC, чтобы сравнения не падали."""
    if value is None:
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def _totals_by_user(
    db: Session,
    challenge: Challenge,
    user_ids: list[int],
) -> dict[int, float]:
    """Суммы метрики челленджа по каждому участнику одним запросом."""
    metric = _METRICS.get((challenge.challenge_type or "").strip().lower())
    if metric is None or not user_ids:
        return {}

    model, time_column, aggregate = metric
    start = _as_utc(challenge.start_date)
    end = _as_utc(challenge.end_date)
    # Считаем только по завершённой части окна: будущие дни ещё не наступили,
    # а прошедшие после end_date в зачёт не идут.
    now = datetime.now(timezone.utc)
    if end is None or end > now:
        end = now

    query = db.query(model.user_id, aggregate).filter(model.user_id.in_(user_ids))
    if start is not None:
        query = query.filter(time_column >= start)
    if end is not None:
        query = query.filter(time_column <= end)

    rows = query.group_by(model.user_id).all()
    return {user_id: float(total or 0) for user_id, total in rows}


def refresh_challenge(db: Session, challenge: Challenge, *, commit: bool = True) -> None:
    """Пересчитать прогресс всех участников одного челленджа."""
    participants = (
        db.query(ChallengeParticipant)
        .filter(ChallengeParticipant.challenge_id == challenge.id)
        .all()
    )
    if not participants:
        return

    totals = _totals_by_user(db, challenge, [p.user_id for p in participants])
    changed = False
    for participant in participants:
        value = int(round(totals.get(participant.user_id, 0.0)))
        if participant.progress != value:
            participant.progress = value
            changed = True

    if changed and commit:
        db.commit()


def refresh_for_user(db: Session, user_id: int, *, commit: bool = True) -> None:
    """Пересчитать прогресс пользователя во всех его активных челленджах.

    Вызывается после появления новых записей дневника, поэтому берём только
    челленджи, окно которых ещё не закрылось.
    """
    now = datetime.now(timezone.utc)
    challenges = (
        db.query(Challenge)
        .join(ChallengeParticipant, ChallengeParticipant.challenge_id == Challenge.id)
        .filter(
            ChallengeParticipant.user_id == user_id,
            Challenge.end_date >= now,
        )
        .all()
    )
    if not challenges:
        return

    changed = False
    for challenge in challenges:
        totals = _totals_by_user(db, challenge, [user_id])
        value = int(round(totals.get(user_id, 0.0)))
        participant = (
            db.query(ChallengeParticipant)
            .filter(
                ChallengeParticipant.challenge_id == challenge.id,
                ChallengeParticipant.user_id == user_id,
            )
            .first()
        )
        if participant is not None and participant.progress != value:
            participant.progress = value
            changed = True

    if changed and commit:
        db.commit()
