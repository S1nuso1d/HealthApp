"""Публичное имя и валидация никнейма для сообщества."""

from __future__ import annotations

import re

from app.models.profile import UserProfile
from app.models.user import User

NICKNAME_RE = re.compile(r"^[a-zA-Z0-9_\u0400-\u04FF]{3,32}$")


def validate_nickname(value: str | None) -> str | None:
    if value is None:
        return None
    n = value.strip()
    if not n:
        return None
    if not NICKNAME_RE.match(n):
        raise ValueError(
            "Никнейм: от 3 до 32 символов, только буквы (латиница/кириллица), цифры и _"
        )
    return n


def public_display_name(profile: UserProfile | None, user: User) -> str:
    if profile:
        nick = (profile.nickname or "").strip()
        if nick:
            return nick
        first = (profile.first_name or "").strip()
        last = (profile.last_name or "").strip()
        full = f"{first} {last}".strip()
        if full:
            return full
    return _masked_email(user)


def _masked_email(user: User) -> str:
    email = user.email or ""
    if "@" in email:
        local, domain = email.split("@", 1)
        return local[:3] + "***@" + domain
    return f"user{user.id}"
