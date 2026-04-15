import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    PROJECT_NAME: str = os.getenv("PROJECT_NAME", "HealthApp API")

    DATABASE_URL: str = os.getenv(
        "DATABASE_URL",
        "sqlite:///./healthapp.db"
    )

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


settings = Settings()