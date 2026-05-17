from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.profile import UserProfile
from app.models.user import User
from app.schemas.profile import ProfileCreate, ProfileResponse
from app.services.avatar_storage import (
    delete_avatar_file,
    find_existing_avatar_path,
    guess_media_type,
    save_avatar,
    validate_avatar_bytes,
)

router = APIRouter(prefix="/profile", tags=["Profile"])


@router.get(
    "/me",
    response_model=ProfileResponse,
    summary="Получить мой профиль",
    description="Возвращает профиль текущего авторизованного пользователя.",
    response_description="Профиль пользователя",
)
def get_my_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile:
        profile = UserProfile(user_id=current_user.id, has_avatar=False)
        db.add(profile)
        db.commit()
        db.refresh(profile)
    return profile


@router.get(
    "/me/avatar",
    summary="Скачать фото профиля",
    description="Возвращает бинарный файл изображения (только при has_avatar=true).",
    responses={404: {"description": "Аватар не загружен"}},
)
def get_my_avatar(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile or not profile.has_avatar:
        raise HTTPException(status_code=404, detail="Аватар не найден")
    path = find_existing_avatar_path(current_user.id)
    if not path:
        profile.has_avatar = False
        db.commit()
        raise HTTPException(status_code=404, detail="Аватар не найден")
    return FileResponse(
        path,
        media_type=guess_media_type(path),
        filename=path.name,
    )


@router.post(
    "/me/avatar",
    response_model=ProfileResponse,
    summary="Загрузить фото профиля",
    description="Принимает multipart с полем file (JPEG, PNG или WEBP, до 5 МБ).",
)
async def upload_my_avatar(
    file: UploadFile = File(..., description="Изображение аватара"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Профиль не найден")

    content = await file.read()
    ok, detail = validate_avatar_bytes(content)
    if not ok:
        raise HTTPException(status_code=400, detail=detail)

    fmt = detail  # при успехе — строка формата jpeg/png/webp
    save_avatar(current_user.id, content, fmt)
    profile.has_avatar = True
    db.commit()
    db.refresh(profile)
    return profile


@router.delete(
    "/me/avatar",
    response_model=ProfileResponse,
    summary="Удалить фото профиля",
)
def delete_my_avatar(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Профиль не найден")
    delete_avatar_file(current_user.id)
    profile.has_avatar = False
    db.commit()
    db.refresh(profile)
    return profile


@router.put(
    "/me",
    response_model=ProfileResponse,
    summary="Обновить мой профиль",
    description="Обновляет данные профиля текущего пользователя.",
    response_description="Обновленный профиль",
)
def update_my_profile(
    profile_data: ProfileCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    profile = db.query(UserProfile).filter(UserProfile.user_id == current_user.id).first()
    if not profile:
        raise HTTPException(status_code=404, detail="Профиль не найден")

    for field, value in profile_data.model_dump(exclude_unset=True).items():
        setattr(profile, field, value)

    db.commit()
    db.refresh(profile)
    return profile
