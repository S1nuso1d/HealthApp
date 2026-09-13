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


DEV_SECRET_KEY = "super-secret-key"


def _env_bool(name: str, default: bool) -> bool:
    raw = _env_strip(name)
    if not raw:
        return default
    return raw.lower() in {"1", "true", "yes", "on"}


def _env_list(name: str, default: list[str]) -> list[str]:
    raw = _env_strip(name)
    if not raw:
        return list(default)
    return [item.strip() for item in raw.split(",") if item.strip()]


class Settings:
    PROJECT_NAME: str = os.getenv("PROJECT_NAME", "HealthApp API")

    # dev | production. В production небезопасные дефолты приводят к отказу старта.
    APP_ENV: str = (_env_strip("APP_ENV") or "dev").lower()

    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "sqlite:///./healthapp.db"
    )

    # Не меняйте между перезапусками, если не хотите инвалидировать все JWT в клиентах.
    SECRET_KEY: str = os.getenv("SECRET_KEY", DEV_SECRET_KEY)
    ALGORITHM: str = os.getenv("ALGORITHM", "HS256")
    ACCESS_TOKEN_EXPIRE_MINUTES: int = int(
        os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", "60")
    )
    REFRESH_TOKEN_EXPIRE_DAYS: int = int(
        os.getenv("REFRESH_TOKEN_EXPIRE_DAYS", "30")
    )

    # CORS: в dev — «*», в production перечисли домены через запятую в ALLOWED_ORIGINS.
    ALLOWED_ORIGINS: list[str] = _env_list("ALLOWED_ORIGINS", ["*"])

    # Ограничение частоты запросов (in-process, на один воркер uvicorn)
    RATE_LIMIT_ENABLED: bool = _env_bool("RATE_LIMIT_ENABLED", True)
    # Формат "запросов/секунд" для чувствительных эндпоинтов
    RATE_LIMIT_AUTH: str = _env_strip("RATE_LIMIT_AUTH") or "10/60"
    RATE_LIMIT_UPLOAD: str = _env_strip("RATE_LIMIT_UPLOAD") or "30/60"
    RATE_LIMIT_AI: str = _env_strip("RATE_LIMIT_AI") or "20/60"

    # Логи: json удобен для сборщиков (Loki/ELK), text — для локальной разработки
    LOG_LEVEL: str = (_env_strip("LOG_LEVEL") or "INFO").upper()
    LOG_FORMAT: str = (_env_strip("LOG_FORMAT") or "text").lower()

    @property
    def is_production(self) -> bool:
        return self.APP_ENV in {"production", "prod"}

    # LLM / Ollama
    LLM_ENABLED: bool = os.getenv("LLM_ENABLED", "true").lower() == "true"
    LLM_PROVIDER: str = os.getenv("LLM_PROVIDER", "ollama")
    LLM_BASE_URL: str = os.getenv(
        "LLM_BASE_URL",
        "http://127.0.0.1:11434",
    )
    LLM_MODEL_NAME: str = os.getenv("LLM_MODEL_NAME", "qwen2.5:14b")
    # Отдельная модель для плана питания (7b быстрее 14b в 2–3 раза)
    LLM_MEAL_PLAN_MODEL: str = _env_strip("LLM_MEAL_PLAN_MODEL") or os.getenv(
        "LLM_MODEL_NAME", "qwen2.5:14b"
    )
    LLM_TIMEOUT_SECONDS: int = int(os.getenv("LLM_TIMEOUT_SECONDS", "120"))
    LLM_TEMPERATURE: float = float(os.getenv("LLM_TEMPERATURE", "0.3"))
    LLM_API_KEY: str = _env_strip("LLM_API_KEY")

    # AI Safety / fallback
    AI_FALLBACK_ENABLED: bool = os.getenv(
        "AI_FALLBACK_ENABLED",
        "true"
    ).lower() == "true"
    AI_MAX_PROMPT_CHARS: int = int(os.getenv("AI_MAX_PROMPT_CHARS", "16000"))

    # Локальная таймзона для советов ИИ (сон, вода, активность по времени суток)
    APP_TIMEZONE: str = os.getenv("APP_TIMEZONE", "Europe/Moscow")

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
    FOOD_IMAGES_DIR_PATH: Path = Path(
        os.getenv("FOOD_IMAGES_DIR", str(_BACKEND_ROOT / "uploads" / "food_images"))
    )
    # Картинки для распознавания блюда уходят в LLM — ограничиваем до аплоада
    FOOD_RECOGNITION_MAX_BYTES: int = int(
        os.getenv("FOOD_RECOGNITION_MAX_BYTES", str(8 * 1024 * 1024))
    )


settings = Settings()


def validate_settings(current: Settings = settings) -> list[str]:
    """Возвращает список проблем конфигурации.

    В production небезопасные значения — фатальны (см. `enforce_production_settings`),
    в dev остаются предупреждениями, чтобы не мешать локальному запуску.
    """
    problems: list[str] = []
    if current.SECRET_KEY == DEV_SECRET_KEY:
        problems.append(
            "SECRET_KEY равен дефолтному значению. Задай случайный ключ в .env: "
            "SECRET_KEY=$(python -c \"import secrets; print(secrets.token_urlsafe(48))\")"
        )
    elif len(current.SECRET_KEY) < 32:
        problems.append("SECRET_KEY короче 32 символов — используй длинный случайный ключ")
    if "*" in current.ALLOWED_ORIGINS:
        problems.append(
            "ALLOWED_ORIGINS разрешает любой источник. Перечисли домены через запятую."
        )
    if current.DATABASE_URL.startswith("sqlite"):
        problems.append(
            "DATABASE_URL указывает на SQLite. Для production используй PostgreSQL."
        )
    return problems


def enforce_production_settings(current: Settings = settings) -> None:
    """Падает при старте, если production запускают с dev-значениями."""
    if not current.is_production:
        return
    problems = validate_settings(current)
    if problems:
        listed = "\n  - ".join(problems)
        raise RuntimeError(
            "APP_ENV=production, но конфигурация небезопасна:\n  - " + listed
        )