import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy import text

from app.core.config import (
    enforce_production_settings,
    settings,
    validate_settings,
)
from app.core.observability import (
    REQUEST_ID_HEADER,
    configure_logging,
    install_exception_handlers,
    install_middleware,
)

import app.models  # noqa: F401 — регистрация моделей в metadata
from app.db.database import Base, SessionLocal, engine
from app.db.schema_patches import apply_lightweight_schema_patches
from app.services.refresh_token_service import purge_expired

API_VERSION = "1.1.0"
from app.api.auth import router as auth_router
from app.api.profile import router as profile_router
from app.api.sleep import router as sleep_router
from app.api.hydration import router as hydration_router
from app.api.meal import router as meal_router
from app.api.food_catalog import router as food_catalog_router
from app.api.activity import router as activity_router
from app.api.states import router as states_router
from app.api.ai import router as ai_router
from app.api.analytics import router as analytics_router
from app.api.smart import router as smart_router
from app.api.action_plan import router as action_plan_router
from app.api.dashboard import router as dashboard_router
from app.api.ws import router as ws_router
from app.api.data_export import router as data_export_router
from app.api.data_import import router as data_import_router
from app.api.integrations import router as integrations_router
from app.api.health import router as health_router
from app.api.gamification import router as gamification_router
from app.api.social import router as social_router
from app.api.pills import router as pills_router
from app.api.cycle import router as cycle_router


logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    configure_logging()
    enforce_production_settings()
    for problem in validate_settings():
        logger.warning("конфигурация: %s", problem)

    settings.AVATAR_DIR_PATH.mkdir(parents=True, exist_ok=True)
    settings.FOOD_IMAGES_DIR_PATH.mkdir(parents=True, exist_ok=True)
    apply_lightweight_schema_patches()

    db = SessionLocal()
    try:
        removed = purge_expired(db)
        if removed:
            logger.info("удалено просроченных записей refresh-токенов: %s", removed)
    except Exception:
        logger.warning("не удалось очистить просроченные refresh-токены", exc_info=True)
    finally:
        db.close()

    logger.info("HealthApp API запущен, окружение=%s", settings.APP_ENV)
    yield


app = FastAPI(
    title=settings.PROJECT_NAME,
    lifespan=lifespan,
    version=API_VERSION,
    # В production скрываем интерактивную документацию: она раскрывает всю схему API
    docs_url=None if settings.is_production else "/docs",
    redoc_url=None if settings.is_production else "/redoc",
    openapi_url=None if settings.is_production else "/openapi.json",
)

Base.metadata.create_all(bind=engine)

install_middleware(app)
install_exception_handlers(app)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    # With wildcard origins, credentials must be disabled (browsers will reject otherwise).
    allow_credentials="*" not in settings.ALLOWED_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=[REQUEST_ID_HEADER],
)

app.include_router(auth_router)
app.include_router(profile_router)
app.include_router(sleep_router)
app.include_router(hydration_router)
app.include_router(meal_router)
app.include_router(food_catalog_router)
app.include_router(activity_router)
app.include_router(states_router)
app.include_router(ai_router)
app.include_router(analytics_router)
app.include_router(smart_router)
app.include_router(action_plan_router)
app.include_router(dashboard_router)
app.include_router(ws_router)
app.include_router(data_import_router)
app.include_router(data_export_router)
app.include_router(integrations_router)
app.include_router(health_router)
app.include_router(gamification_router)
app.include_router(social_router)
app.include_router(pills_router)
app.include_router(cycle_router)


@app.get("/", tags=["Service"], summary="Проверка, что API отвечает")
def root():
    return {"message": "HealthApp API is running", "version": API_VERSION}


@app.get("/healthz", tags=["Service"], summary="Health check для мониторинга")
def healthz():
    """Проверяет и процесс, и доступность БД — иначе балансировщик считает живым
    сервер, у которого отвалилась база."""
    database_ok = True
    try:
        with engine.connect() as conn:
            conn.execute(text("SELECT 1"))
    except Exception:
        database_ok = False
        logger.warning("healthz: база недоступна", exc_info=True)

    return JSONResponse(
        status_code=200 if database_ok else 503,
        content={
            "status": "ok" if database_ok else "degraded",
            "version": API_VERSION,
            "environment": settings.APP_ENV,
            "database": "ok" if database_ok else "unavailable",
        },
    )