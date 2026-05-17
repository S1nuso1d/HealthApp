"""Сохранение и отдача файлов аватара (JPEG / PNG / WEBP)."""

from __future__ import annotations

import mimetypes
from pathlib import Path
from typing import Optional, Tuple

from app.core.config import settings


def _detect_format(content: bytes) -> Optional[str]:
    if len(content) < 12:
        return None
    if content[:3] == b"\xff\xd8\xff":
        return "jpeg"
    if content[:8] == b"\x89PNG\r\n\x1a\n":
        return "png"
    if content[:4] == b"RIFF" and content[8:12] == b"WEBP":
        return "webp"
    return None


def validate_avatar_bytes(content: bytes) -> Tuple[bool, Optional[str]]:
    if len(content) > settings.AVATAR_MAX_BYTES:
        return False, "Файл слишком большой"
    fmt = _detect_format(content)
    if not fmt:
        return False, "Допустимы только изображения JPEG, PNG или WEBP"
    return True, fmt


def avatar_file_path(user_id: int, fmt: str) -> Path:
    return settings.AVATAR_DIR_PATH / f"{user_id}.{fmt}"


def find_existing_avatar_path(user_id: int) -> Optional[Path]:
    for ext in ("jpeg", "png", "webp"):
        p = settings.AVATAR_DIR_PATH / f"{user_id}.{ext}"
        if p.is_file():
            return p
    return None


def save_avatar(user_id: int, content: bytes, fmt: str) -> Path:
    settings.AVATAR_DIR_PATH.mkdir(parents=True, exist_ok=True)
    # Удаляем старый файл с другим расширением
    delete_avatar_file(user_id)
    path = avatar_file_path(user_id, fmt)
    path.write_bytes(content)
    return path


def delete_avatar_file(user_id: int) -> None:
    for ext in ("jpeg", "png", "webp"):
        p = settings.AVATAR_DIR_PATH / f"{user_id}.{ext}"
        if p.is_file():
            p.unlink()


def guess_media_type(path: Path) -> str:
    mt, _ = mimetypes.guess_type(str(path))
    return mt or "application/octet-stream"
