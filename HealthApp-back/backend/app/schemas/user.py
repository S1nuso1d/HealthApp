from pydantic import BaseModel, EmailStr, Field


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

    class Config:
        from_attributes = True


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


class RegisterVerify(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6, max_length=128)
    code: str = Field(..., min_length=4, max_length=16, description="Код из письма")


class RegisterStartResponse(BaseModel):
    message: str = Field(description="Статус отправки кода")


class Token(BaseModel):
    access_token: str = Field(description="JWT токен доступа")
    token_type: str = Field(description="Тип токена, обычно bearer")