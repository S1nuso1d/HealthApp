import uuid
from datetime import datetime, timedelta, timezone

import bcrypt
from jose import JWTError, jwt

from app.core.config import settings

ACCESS_TOKEN_TYPE = "access"
REFRESH_TOKEN_TYPE = "refresh"


class TokenError(Exception):
    """Токен не прошёл проверку подписи, срока действия или типа."""


def get_password_hash(password: str) -> str:
    password_bytes = password.encode("utf-8")
    salt = bcrypt.gensalt()
    hashed = bcrypt.hashpw(password_bytes, salt)
    return hashed.decode("utf-8")


def verify_password(plain_password: str, hashed_password: str) -> bool:
    plain_password_bytes = plain_password.encode("utf-8")
    hashed_password_bytes = hashed_password.encode("utf-8")
    return bcrypt.checkpw(plain_password_bytes, hashed_password_bytes)


def _encode(data: dict, token_type: str, lifetime: timedelta) -> str:
    now = datetime.now(timezone.utc)
    to_encode = data.copy()
    to_encode["exp"] = int((now + lifetime).timestamp())
    to_encode["iat"] = int(now.timestamp())
    to_encode["type"] = token_type
    to_encode["jti"] = uuid.uuid4().hex
    return jwt.encode(to_encode, settings.SECRET_KEY, algorithm=settings.ALGORITHM)


def create_access_token(data: dict) -> str:
    return _encode(
        data,
        ACCESS_TOKEN_TYPE,
        timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES),
    )


def create_refresh_token(data: dict) -> str:
    return _encode(
        data,
        REFRESH_TOKEN_TYPE,
        timedelta(days=settings.REFRESH_TOKEN_EXPIRE_DAYS),
    )


def decode_token(token: str, expected_type: str) -> dict:
    """Проверяет подпись, срок действия и назначение токена.

    Без проверки `type` access-токен принимался бы там, где ожидается refresh
    (и наоборот) — на 30 дней вместо часа.
    """
    try:
        payload = jwt.decode(
            token,
            settings.SECRET_KEY,
            algorithms=[settings.ALGORITHM],
        )
    except JWTError as exc:
        raise TokenError("Подпись токена недействительна или срок истёк") from exc

    token_type = payload.get("type")
    if token_type is None:
        # Токены, выпущенные до появления claim `type`: считаем их access-токенами,
        # чтобы уже установленные приложения не разлогинились при обновлении сервера.
        if expected_type != ACCESS_TOKEN_TYPE:
            raise TokenError("Ожидался токен типа refresh")
    elif token_type != expected_type:
        raise TokenError(f"Ожидался токен типа {expected_type}, получен {token_type}")

    return payload


def user_id_from_token(token: str, expected_type: str = ACCESS_TOKEN_TYPE) -> int:
    payload = decode_token(token, expected_type)
    subject = payload.get("sub")
    if subject is None:
        raise TokenError("В токене нет идентификатора пользователя")
    try:
        return int(subject)
    except (TypeError, ValueError) as exc:
        raise TokenError("Идентификатор пользователя в токене повреждён") from exc


def token_expiry(payload: dict) -> datetime:
    exp = payload.get("exp")
    if exp is None:
        return datetime.now(timezone.utc) + timedelta(
            days=settings.REFRESH_TOKEN_EXPIRE_DAYS
        )
    return datetime.fromtimestamp(int(exp), tz=timezone.utc)
