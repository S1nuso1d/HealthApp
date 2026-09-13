"""Ротация и отзыв refresh-токенов.

Токены остаются stateless (JWT), но каждый потраченный `jti` сохраняется в БД.
Это даёт две вещи, которых не было раньше: одноразовость refresh-токена и
возможность разлогинить пользователя, не меняя SECRET_KEY.
"""

import logging
from datetime import datetime, timezone

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.refresh_token import RevokedRefreshToken
from app.models.user import User

logger = logging.getLogger(__name__)


class RefreshTokenReuse(Exception):
    """Refresh-токен предъявлен повторно — вероятная компрометация."""


def _as_utc(value: datetime | None) -> datetime | None:
    if value is None:
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value


def is_revoked(db: Session, jti: str) -> bool:
    if not jti:
        return False
    return (
        db.query(RevokedRefreshToken.id)
        .filter(RevokedRefreshToken.jti == jti)
        .first()
        is not None
    )


def issued_before_cutoff(user: User, issued_at: int | None) -> bool:
    """True, если токен выпущен до принудительного отзыва всех токенов."""
    cutoff = _as_utc(user.tokens_valid_from)
    if cutoff is None:
        return False
    if issued_at is None:
        return True
    return datetime.fromtimestamp(int(issued_at), tz=timezone.utc) < cutoff


def revoke(
    db: Session,
    *,
    jti: str,
    user_id: int,
    expires_at: datetime,
    reason: str = "rotated",
) -> None:
    """Помечает конкретный refresh-токен как потраченный."""
    if not jti:
        return
    db.add(
        RevokedRefreshToken(
            jti=jti,
            user_id=user_id,
            expires_at=expires_at,
            revoked_at=datetime.now(timezone.utc),
            reason=reason,
        )
    )
    try:
        db.commit()
    except IntegrityError:
        # Параллельный запрос успел записать тот же jti — состояние уже верное
        db.rollback()


def revoke_all_for_user(db: Session, user: User, reason: str = "revoked_all") -> None:
    """Отзывает все refresh-токены пользователя, сдвигая границу выпуска."""
    user.tokens_valid_from = datetime.now(timezone.utc)
    db.commit()
    logger.info("refresh tokens revoked for user_id=%s reason=%s", user.id, reason)


def purge_expired(db: Session) -> int:
    """Удаляет записи о просроченных токенах — держать их незачем."""
    deleted = (
        db.query(RevokedRefreshToken)
        .filter(RevokedRefreshToken.expires_at < datetime.now(timezone.utc))
        .delete(synchronize_session=False)
    )
    db.commit()
    return deleted
