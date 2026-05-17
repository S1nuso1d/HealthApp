from datetime import date

from pydantic import BaseModel


class CopyDayRequest(BaseModel):
    """Скопировать все приёмы пищи с source_date на target_date (по умолчанию — сегодня)."""

    source_date: date
    target_date: date | None = None
