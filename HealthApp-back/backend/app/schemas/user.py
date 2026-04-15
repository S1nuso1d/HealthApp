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


class Token(BaseModel):
    access_token: str = Field(description="JWT токен доступа")
    token_type: str = Field(description="Тип токена, обычно bearer")