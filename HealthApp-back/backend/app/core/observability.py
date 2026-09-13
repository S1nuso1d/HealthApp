"""Логирование, идентификатор запроса и единый формат ошибок.

До этого у API не было ни сквозного request-id, ни централизованной настройки
логов: разобрать, какой запрос привёл к трассировке, было нельзя.
"""

import json
import logging
import time
import uuid
from contextvars import ContextVar

from fastapi import FastAPI, Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.config import settings

REQUEST_ID_HEADER = "X-Request-ID"

_request_id: ContextVar[str] = ContextVar("request_id", default="-")

logger = logging.getLogger("healthapp.access")


def current_request_id() -> str:
    return _request_id.get()


class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = _request_id.get()
        return True


class JsonLogFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        payload = {
            "ts": self.formatTime(record, "%Y-%m-%dT%H:%M:%S"),
            "level": record.levelname,
            "logger": record.name,
            "request_id": getattr(record, "request_id", "-"),
            "message": record.getMessage(),
        }
        for key in ("method", "path", "status_code", "duration_ms"):
            value = getattr(record, key, None)
            if value is not None:
                payload[key] = value
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)


def configure_logging() -> None:
    if settings.LOG_FORMAT == "json":
        formatter: logging.Formatter = JsonLogFormatter()
    else:
        formatter = logging.Formatter(
            "%(asctime)s %(levelname)-8s [%(request_id)s] %(name)s: %(message)s"
        )

    handler = logging.StreamHandler()
    handler.setFormatter(formatter)
    handler.addFilter(RequestIdFilter())

    root = logging.getLogger()
    # Меняем обработчики только своей настройки, чтобы повторный вызов не дублировал вывод
    for existing in list(root.handlers):
        root.removeHandler(existing)
    root.addHandler(handler)
    root.setLevel(getattr(logging, settings.LOG_LEVEL, logging.INFO))

    # uvicorn пишет свой access-лог без request_id — наш middleware информативнее
    logging.getLogger("uvicorn.access").disabled = True
    logging.getLogger("uvicorn.error").handlers = [handler]


def install_middleware(app: FastAPI) -> None:
    @app.middleware("http")
    async def request_context(request: Request, call_next):
        incoming = request.headers.get(REQUEST_ID_HEADER, "").strip()
        request_id = incoming or uuid.uuid4().hex[:16]
        token = _request_id.set(request_id)
        started = time.perf_counter()
        try:
            response = await call_next(request)
        except Exception:
            duration_ms = round((time.perf_counter() - started) * 1000, 1)
            logger.exception(
                "unhandled error",
                extra={
                    "method": request.method,
                    "path": request.url.path,
                    "duration_ms": duration_ms,
                },
            )
            raise
        else:
            duration_ms = round((time.perf_counter() - started) * 1000, 1)
            response.headers[REQUEST_ID_HEADER] = request_id
            logger.info(
                "%s %s -> %s",
                request.method,
                request.url.path,
                response.status_code,
                extra={
                    "method": request.method,
                    "path": request.url.path,
                    "status_code": response.status_code,
                    "duration_ms": duration_ms,
                },
            )
            return response
        finally:
            _request_id.reset(token)


def install_exception_handlers(app: FastAPI) -> None:
    """Единый JSON-конверт ошибок с request_id, чтобы клиент мог его показать в багрепорте."""

    def envelope(status_code: int, detail, request_id: str) -> JSONResponse:
        return JSONResponse(
            status_code=status_code,
            content={"detail": detail, "request_id": request_id},
            headers={REQUEST_ID_HEADER: request_id},
        )

    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        response = envelope(exc.status_code, exc.detail, current_request_id())
        for key, value in (exc.headers or {}).items():
            response.headers[key] = value
        return response

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        return envelope(
            status.HTTP_422_UNPROCESSABLE_ENTITY,
            exc.errors(),
            current_request_id(),
        )

    @app.exception_handler(Exception)
    async def unhandled_exception_handler(request: Request, exc: Exception):
        request_id = current_request_id()
        logger.exception("unhandled exception on %s %s", request.method, request.url.path)
        # Текст исключения наружу не отдаём — в нём бывают пути и параметры запросов
        return envelope(
            status.HTTP_500_INTERNAL_SERVER_ERROR,
            "Внутренняя ошибка сервера. Сообщите этот идентификатор в поддержку: " + request_id,
            request_id,
        )
