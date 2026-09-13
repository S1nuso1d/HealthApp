from typing import Optional

from pydantic import BaseModel, ConfigDict, EmailStr, Field


class UserCreate(BaseModel):
    email: EmailStr = Field(
        ...,
        description="Email пользователя",
        examples=["test@test.com"]
    )
    password: str = Field(
        ...,
        min_length=6,
        max_length=128,
        description="Пароль пользователя, минимум 6 символов",
        examples=["12345678"]
    )


class UserResponse(BaseModel):
    id: int = Field(description="ID пользователя")
    email: EmailStr = Field(description="Email пользователя")
    is_active: bool = Field(description="Активен ли пользователь")

    model_config = ConfigDict(from_attributes=True)


class PasswordConfirmBody(BaseModel):
    password: str = Field(..., min_length=1, description="Текущий пароль для подтверждения удаления")


class ForgotPasswordBody(BaseModel):
    email: EmailStr = Field(..., description="Email аккаунта для сброса пароля")


class ChangePasswordBody(BaseModel):
    current_password: str = Field(..., min_length=1, description="Текущий пароль")
    new_password: str = Field(
        ...,
        min_length=6,
        max_length=128,
        description="Новый пароль (не короче 6 символов)",
    )


class RefreshTokenRequest(BaseModel):
    refresh_token: str = Field(..., description="Refresh токен")


class RegisterVerify(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6, max_length=128)
    code: str = Field(..., min_length=4, max_length=16, description="Код из письма")
    profile: Optional["RegisterProfileDraft"] = None


class RegisterProfileDraft(BaseModel):
    first_name: Optional[str] = Field(None, max_length=64)
    last_name: Optional[str] = Field(None, max_length=64)
    nickname: Optional[str] = Field(None, max_length=32)
    age: Optional[int] = Field(None, ge=1, le=120)
    birth_date: Optional[str] = Field(None, max_length=10)
    sex: Optional[str] = Field(None, max_length=16)
    height_cm: Optional[float] = Field(None, ge=80, le=260)
    weight_kg: Optional[float] = Field(None, ge=25, le=400)
    goal: Optional[str] = Field(None, max_length=64)
    activity_level: Optional[str] = Field(None, max_length=32)
    is_vegetarian: Optional[bool] = None
    has_allergies: Optional[bool] = None
    allergies_text: Optional[str] = Field(None, max_length=500)


class NicknameCheckResponse(BaseModel):
    available: bool = Field(description="Можно ли использовать никнейм")
    message: Optional[str] = Field(None, description="Причина, если занят или неверный формат")


RegisterVerify.model_rebuild()


class RegisterStartResponse(BaseModel):
    message: str = Field(description="Статус отправки кода")


class Token(BaseModel):
    access_token: str = Field(description="JWT токен доступа")
    refresh_token: str = Field(description="JWT токен обновления")
    token_type: str = Field(description="Тип токена, обычно bearer")