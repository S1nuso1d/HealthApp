import os
from pathlib import Path

from dotenv import load_dotenv

# Корень пакета backend (каталог, где лежит папка `app/`) — сюда же клади `.env`
_BACKEND_ROOT = Path(__file__).resolve().parent.parent.parent

# Явные пути (не зависят от cwd). Сначала родитель, потом backend — значения из `backend/.env` важнее.
# override=True: пустая SMTP_HOST в системных переменных IDE не «перебивает» файл .env
load_dotenv(_BACKEND_ROOT.parent / ".env", override=True)
load_dotenv(_BACKEND_ROOT / ".env", override=True)


def _env_strip(name: str, default: str = "") -> str:
    """Убирает пробелы и BOM — частая причина «ключи в .env есть, а os.getenv пустой»."""
    raw = os.getenv(name, default)
    if raw is None:
        return ""
    return str(raw).strip().strip("\ufeff")


class Settings:
    PROJECT_NAME: str = os.getenv("PROJECT_NAME", "HealthApp API")

    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "sqlite:///./healthapp.db"
    )

    # Не меняйте между перезапусками, если не хотите инвалидировать все JWT в клиентах.
    SECRET_KEY: str = os.getenv("SECRET_KEY", "super-secret-key")
    ALGORITHM: str = os.getenv("ALGORITHM", "HS256")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = int(
        os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "60")
    )

    # LLM / Ollama
    LLM_ENABLED: bool = os.getenv("LLM_ENABLED", "true").lower() == "true"
    LLM_PROVIDER: str = os.getenv("LLM_PROVIDER", "ollama")
    LLM_BASE_URL: str = os.getenv(
        "LLM_BASE_URL",
        "http://localhost:11434/api/generate"
    )
    LLM_MODEL_NAME: str = os.getenv("LLM_MODEL_NAME", "gemma:2b")
    LLM_TIMEOUT_SECONDS: int = int(os.getenv("LLM_TIMEOUT_SECONDS", "120"))
    LLM_TEMPERATURE: float = float(os.getenv("LLM_TEMPERATURE", "0.3"))

    # AI Safety / fallback
    AI_FALLBACK_ENABLED: bool = os.getenv(
        "AI_FALLBACK_ENABLED",
        "true"
    ).lower() == "true"
    AI_MAX_PROMPT_CHARS: int = int(os.getenv("AI_MAX_PROMPT_CHARS", "12000"))

    # FatSecret Platform (OAuth 1.0) — ключи приложения с https://platform.fatsecret.com/api/
    # Публичные методы (foods.search и т.д.) подписываются только consumer key/secret.
    FATSECRET_CONSUMER_KEY: str = _env_strip("FATSECRET_CONSUMER_KEY")
    FATSECRET_CONSUMER_SECRET: str = _env_strip("FATSECRET_CONSUMER_SECRET")

    # Подтверждение email при регистрации (если SMTP_HOST пустой — код только в логах сервера)
    SMTP_HOST: str = os.getenv("SMTP_HOST", "")
    SMTP_PORT: int = int(os.getenv("SMTP_PORT", "587"))
    SMTP_USER: str = os.getenv("SMTP_USER", "")
    SMTP_PASSWORD: str = os.getenv("SMTP_PASSWORD", "")
    SMTP_FROM: str = os.getenv("SMTP_FROM", "noreply@healthapp.local")
    SMTP_USE_TLS: bool = os.getenv("SMTP_USE_TLS", "true").lower() == "true"
    # True = smtplib.SMTP_SSL (порт 465 у Mail.ru). Для 587 оставь false.
    SMTP_USE_SSL: bool = os.getenv("SMTP_USE_SSL", "false").lower() == "true"
    REGISTRATION_CODE_TTL_MINUTES: int = int(os.getenv("REGISTRATION_CODE_TTL_MINUTES", "15"))

    # Аватары: файлы на диске, раздача только авторизованным GET /profile/me/avatar
    AVATAR_DIR_PATH: Path = Path(
        os.getenv("AVATAR_DIR", str(_BACKEND_ROOT / "uploads" / "avatars"))
    )
    AVATAR_MAX_BYTES: int = int(os.getenv("AVATAR_MAX_BYTES", str(5 * 1024 * 1024)))


settings = Settings()