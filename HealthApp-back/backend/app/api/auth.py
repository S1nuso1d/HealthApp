import logging
import secrets
import smtplib
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.core.config import settings
from app.core.security import (
    create_access_token,
    get_password_hash,
    verify_password,
)
from app.db.database import get_db
from app.models.pending_registration import PendingRegistration
from app.models.profile import UserProfile
from app.models.user import User
from app.schemas.user import (
    ChangePasswordBody,
    ForgotPasswordBody,
    PasswordConfirmBody,
    RegisterStartResponse,
    RegisterVerify,
    Token,
    UserCreate,
)
from app.services.account_deletion import delete_user_and_related_data
from app.services.registration_email import (
    send_password_reset_email,
    send_registration_verification_email,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/auth", tags=["Auth"])


def _smtp_failure_user_message(exc: BaseException) -> str:
    """Человекочитаемое сообщение для ответа API при ошибке SMTP."""
    if isinstance(exc, smtplib.SMTPAuthenticationError) and exc.args:
        code = exc.args[0]
        resp = exc.args[1] if len(exc.args) > 1 else b""
        if code == 535 and isinstance(resp, (bytes, bytearray)):
            low = resp.lower()
            if b"parol prilozheniya" in low or b"application password" in low:
                return (
                    "Mail.ru: для отправки через SMTP нужен «пароль приложения» "
                    "(не тот пароль, что для входа в почту в браузере). "
                    "Создай его: https://help.mail.ru/mail/security/protection/external "
                    "и укажи в backend/.env в SMTP_PASSWORD. Если в пароле есть символы вроде +, "
                    "заключи значение в кавычки: SMTP_PASSWORD=\"...\"."
                )
    return f"Не удалось отправить письмо: {exc!s}"


def _normalize_code(raw: str) -> str:
    digits = "".join(c for c in (raw or "").strip() if c.isdigit())
    if not digits:
        return ""
    tail = digits[-6:]
    return tail.zfill(6)


@router.post(
    "/register/start",
    response_model=RegisterStartResponse,
    summary="Начать регистрацию — отправить код на email",
)
def register_start(user_data: UserCreate, db: Session = Depends(get_db)):
    email = str(user_data.email).lower().strip()

    existing = db.query(User).filter(User.email == email).first()
    if existing:
        logger.info(
            "register_start: 400 — пользователь уже есть (%s). Нужен вход, а не повторная регистрация.",
            email,
        )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                "Этот email уже зарегистрирован. Открой экран входа и войди с этим email и паролем. "
                "Если забыл пароль — на экране входа нажми «Забыли пароль?»."
            ),
        )

    if settings.SMTP_HOST.strip():
        if not settings.SMTP_USER.strip() or not settings.SMTP_PASSWORD.strip():
            logger.warning(
                "register_start: 400 — в .env задан SMTP_HOST=%r, но пустой SMTP_USER или SMTP_PASSWORD",
                settings.SMTP_HOST,
            )
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="В backend/.env задан SMTP_HOST, но пусты SMTP_USER или SMTP_PASSWORD. Проверь файл и перезапусти сервер.",
            )

    code = f"{secrets.randbelow(1_000_000):06d}"
    now = datetime.now(timezone.utc)
    expires = now + timedelta(minutes=settings.REGISTRATION_CODE_TTL_MINUTES)

    hashed = get_password_hash(user_data.password)

    db.query(PendingRegistration).filter(PendingRegistration.email == email).delete()
    pending = PendingRegistration(
        email=email,
        hashed_password=hashed,
        code=code,
        expires_at=expires,
        created_at=now,
    )
    db.add(pending)
    db.commit()

    try:
        send_registration_verification_email(email, code)
    except Exception as e:
        db.query(PendingRegistration).filter(PendingRegistration.email == email).delete()
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_smtp_failure_user_message(e),
        ) from e

    if settings.SMTP_HOST:
        msg = (
            "Код отправлен на почту. Если письма нет — проверь папку «Спам» "
            "и правильность адреса."
        )
    else:
        msg = (
            "Почта не настроена на сервере (переменная SMTP_HOST пустая): "
            "реального письма не будет. Открой консоль, где запущен backend — "
            "там строка [HealthApp mail dev] с кодом. "
            "Чтобы код приходил на email, задай SMTP_* в .env (см. backend/.env.example)."
        )
    return RegisterStartResponse(message=msg)


@router.post(
    "/register/complete",
    response_model=Token,
    summary="Завершить регистрацию — проверить код и создать аккаунт",
)
def register_complete(body: RegisterVerify, db: Session = Depends(get_db)):
    email = str(body.email).lower().strip()
    code = _normalize_code(body.code)
    if len(code) != 6:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Введи 6-значный код из письма",
        )

    existing = db.query(User).filter(User.email == email).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Пользователь с таким email уже существует",
        )

    pending = db.query(PendingRegistration).filter(PendingRegistration.email == email).first()
    if not pending:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Сначала запроси код регистрации",
        )

    now = datetime.now(timezone.utc)
    if pending.expires_at.tzinfo is None:
        expires = pending.expires_at.replace(tzinfo=timezone.utc)
    else:
        expires = pending.expires_at
    if now > expires:
        db.delete(pending)
        db.commit()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Код истёк — запроси новый",
        )

    if not verify_password(body.password, pending.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Неверный пароль (должен совпадать с указанным при запросе кода)",
        )

    if not secrets.compare_digest(pending.code, code):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Неверный код",
        )

    new_user = User(
        email=email,
        hashed_password=pending.hashed_password,
    )
    db.add(new_user)
    db.delete(pending)
    db.commit()
    db.refresh(new_user)

    profile = UserProfile(user_id=new_user.id)
    db.add(profile)
    db.commit()

    access_token = create_access_token(data={"sub": str(new_user.id)})
    return {
        "access_token": access_token,
        "token_type": "bearer",
    }


@router.post(
    "/login",
    response_model=Token,
    summary="Вход в систему",
    description="OAuth2-логин. В поле username нужно вводить email, в поле password — пароль.",
    response_description="JWT токен доступа",
)
def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    db: Session = Depends(get_db),
):
    user = db.query(User).filter(User.email == form_data.username).first()

    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Неверный email или пароль",
        )

    access_token = create_access_token(data={"sub": str(user.id)})

    return {
        "access_token": access_token,
        "token_type": "bearer",
    }


@router.post(
    "/forgot-password",
    response_model=RegisterStartResponse,
    summary="Сброс пароля — временный пароль на email",
    description="Если аккаунт существует, на почту уходит 8-значный временный пароль. Ответ одинаковый, чтобы не подбирать email.",
)
def forgot_password(body: ForgotPasswordBody, db: Session = Depends(get_db)):
    email = str(body.email).lower().strip()

    if settings.SMTP_HOST.strip():
        if not settings.SMTP_USER.strip() or not settings.SMTP_PASSWORD.strip():
            logger.warning(
                "forgot_password: SMTP_HOST задан, но пусты SMTP_USER или SMTP_PASSWORD",
            )
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="В backend/.env задан SMTP_HOST, но пусты SMTP_USER или SMTP_PASSWORD.",
            )

    public_msg = (
        "Если этот email зарегистрирован в HealthApp, на него отправлен временный пароль из 8 цифр. "
        "Войди с ним и в разделе «Профиль» смени пароль на свой."
    )

    user = db.query(User).filter(User.email == email).first()
    if not user:
        logger.info("forgot_password: email не найден %s — пароль не меняем", email)
        return RegisterStartResponse(message=public_msg)

    plain = f"{secrets.randbelow(10**8):08d}"
    try:
        send_password_reset_email(email, plain)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=_smtp_failure_user_message(e),
        ) from e

    user.hashed_password = get_password_hash(plain)
    db.commit()
    logger.info("forgot_password: отправлен сброс для user_id=%s", user.id)
    return RegisterStartResponse(message=public_msg)


@router.post(
    "/change-password",
    summary="Сменить пароль (нужен вход)",
    description="Текущий пароль и новый (не короче 6 символов).",
)
def change_password(
    body: ChangePasswordBody,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not verify_password(body.current_password, current_user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Неверный текущий пароль",
        )
    if body.current_password == body.new_password:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Новый пароль должен отличаться от текущего",
        )
    current_user.hashed_password = get_password_hash(body.new_password)
    db.commit()
    return {"message": "Пароль успешно изменён"}


@router.post(
    "/delete-account",
    summary="Удалить аккаунт навсегда",
    description="Требует текущий пароль. Удаляет пользователя и все связанные данные.",
)
def delete_account(
    body: PasswordConfirmBody,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    if not verify_password(body.password, current_user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Неверный пароль",
        )
    uid = current_user.id
    delete_user_and_related_data(db, uid)
    return {"message": "Аккаунт удалён"}
