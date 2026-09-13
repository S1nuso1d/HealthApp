from sqlalchemy import Column, DateTime, Integer, String

from app.db.database import Base


class RevokedRefreshToken(Base):
    """Использованные и отозванные refresh-токены (по `jti`).

    Ротация: каждый вызов `/auth/refresh` заносит предъявленный `jti` сюда и выдаёт
    новую пару. Повторное предъявление того же токена — признак кражи, поэтому
    все refresh-токены пользователя отзываются целиком.
    """

    __tablename__ = "revoked_refresh_tokens"

    id = Column(Integer, primary_key=True, index=True)
    jti = Column(String(64), unique=True, index=True, nullable=False)
    user_id = Column(Integer, index=True, nullable=False)
    # Записи старше этого момента можно удалять — токен всё равно просрочен
    expires_at = Column(DateTime(timezone=True), nullable=False)
    revoked_at = Column(DateTime(timezone=True), nullable=False)
    reason = Column(String(32), nullable=False, default="rotated")
