"""Отправка писем: код регистрации, сброс пароля."""

from __future__ import annotations

import logging
import smtplib
import ssl
from email.message import EmailMessage

from app.core.config import settings

logger = logging.getLogger(__name__)

# Для тестов и dev без SMTP
LAST_SENT_CODES: dict[str, str] = {}
LAST_RESET_PASSWORDS: dict[str, str] = {}


def _send_email_via_smtp(msg: EmailMessage) -> None:
    ctx = ssl.create_default_context()
    use_ssl = settings.SMTP_USE_SSL or settings.SMTP_PORT == 465
    if use_ssl:
        with smtplib.SMTP_SSL(
            settings.SMTP_HOST,
            settings.SMTP_PORT,
            timeout=30,
            context=ctx,
        ) as smtp:
            if settings.SMTP_USER:
                smtp.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            smtp.send_message(msg)
    else:
        with smtplib.SMTP(
            settings.SMTP_HOST,
            settings.SMTP_PORT,
            timeout=30,
        ) as smtp:
            smtp.ehlo()
            if settings.SMTP_USE_TLS:
                smtp.starttls(context=ctx)
                smtp.ehlo()
            if settings.SMTP_USER:
                smtp.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            smtp.send_message(msg)


def send_registration_verification_email(to_email: str, code: str) -> None:
    LAST_SENT_CODES[to_email.lower()] = code
    if not settings.SMTP_HOST.strip():
        print(
            f"[HealthApp mail dev] Код подтверждения для {to_email}: {code} "
            f"(действует {settings.REGISTRATION_CODE_TTL_MINUTES} мин.)"
        )
        return

    msg = EmailMessage()
    msg["Subject"] = "Код подтверждения HealthApp"
    msg["From"] = settings.SMTP_FROM
    msg["To"] = to_email
    msg.set_content(
        f"Привет!\n\n"
        f"Твой код подтверждения: {code}\n\n"
        f"Он действует {settings.REGISTRATION_CODE_TTL_MINUTES} минут.\n"
        f"Если ты не регистрировался в HealthApp, просто игнорируй это письмо.\n"
    )

    try:
        _send_email_via_smtp(msg)
    except Exception:
        logger.exception(
            "SMTP: ошибка отправки регистрации (host=%r port=%s)",
            settings.SMTP_HOST,
            settings.SMTP_PORT,
        )
        raise


def send_password_reset_email(to_email: str, temporary_password: str) -> None:
    """Временный пароль из 8 цифр — пользователь входит и меняет пароль в профиле."""
    LAST_RESET_PASSWORDS[to_email.lower()] = temporary_password
    if not settings.SMTP_HOST.strip():
        print(
            f"[HealthApp mail dev] Временный пароль для {to_email}: {temporary_password} "
            f"(войди с ним и смени пароль в разделе «Профиль».)"
        )
        return

    msg = EmailMessage()
    msg["Subject"] = "Новый пароль HealthApp"
    msg["From"] = settings.SMTP_FROM
    msg["To"] = to_email
    msg.set_content(
        f"Привет!\n\n"
        f"Твой новый временный пароль для входа в HealthApp: {temporary_password}\n\n"
        f"Это 8 цифр. Войди в приложение с email {to_email} и этим паролем, "
        f"затем в разделе «Профиль» смени пароль на свой.\n\n"
        f"Если ты не запрашивал сброс — срочно смени пароль в приложении или напиши в поддержку.\n"
    )

    try:
        _send_email_via_smtp(msg)
    except Exception:
        logger.exception(
            "SMTP: ошибка отправки сброса пароля (host=%r port=%s)",
            settings.SMTP_HOST,
            settings.SMTP_PORT,
        )
        raise
