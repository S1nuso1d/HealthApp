from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy.orm import Session

from app.core.security import ACCESS_TOKEN_TYPE, TokenError, decode_token
from app.db.database import get_db
from app.models.user import User

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/auth/login")


def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: Session = Depends(get_db),
) -> User:
    invalid_token = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Недействительный или просроченный токен. Выйдите и войдите снова.",
        headers={"WWW-Authenticate": "Bearer"},
    )
    user_missing = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Пользователь не найден в базе (возможно, сбросили БД на сервере). Зарегистрируйтесь или войдите снова.",
        headers={"WWW-Authenticate": "Bearer"},
    )

    try:
        payload = decode_token(token, ACCESS_TOKEN_TYPE)
        subject = payload.get("sub")
        if subject is None:
            raise invalid_token
        user_id = int(subject)
    except (TokenError, ValueError):
        raise invalid_token

    user = db.query(User).filter(User.id == user_id).first()
    if user is None:
        raise user_missing

    return user
