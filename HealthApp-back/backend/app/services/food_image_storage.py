from __future__ import annotations

from pathlib import Path

from app.core.config import settings
from app.services.avatar_storage import validate_avatar_bytes


def food_images_dir() -> Path:
    path = settings.FOOD_IMAGES_DIR_PATH
    path.mkdir(parents=True, exist_ok=True)
    return path


def food_image_path(catalog_id: int, fmt: str) -> Path:
    return food_images_dir() / f"{catalog_id}.{fmt}"


def find_food_image_path(catalog_id: int) -> Path | None:
    for ext in ("jpeg", "png", "webp"):
        p = food_image_path(catalog_id, ext)
        if p.is_file():
            return p
    return None


def save_food_image(catalog_id: int, content: bytes, fmt: str) -> Path:
    delete_food_image(catalog_id)
    path = food_image_path(catalog_id, fmt)
    path.write_bytes(content)
    return path


def delete_food_image(catalog_id: int) -> None:
    for ext in ("jpeg", "png", "webp"):
        p = food_image_path(catalog_id, ext)
        if p.is_file():
            p.unlink()
