"""Проверки дат записей дневника (нельзя логировать будущие дни)."""

from datetime import date, datetime

from fastapi import HTTPException, status

FUTURE_DATE_DETAIL = "Нельзя добавлять данные за будущие дни"


def ensure_date_not_future(value: date) -> None:
    if value > date.today():
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=FUTURE_DATE_DETAIL,
        )


def ensure_datetime_not_future(value: datetime) -> None:
    ensure_date_not_future(value.date())
