import pytest
from datetime import date, datetime, timedelta
from fastapi import HTTPException

from app.services.date_validation import ensure_date_not_future, ensure_datetime_not_future

def test_ensure_date_not_future_with_past_date():
    past_date = date.today() - timedelta(days=1)
    # Should not raise exception
    ensure_date_not_future(past_date)

def test_ensure_date_not_future_with_today():
    today = date.today()
    # Should not raise exception
    ensure_date_not_future(today)

def test_ensure_date_not_future_with_future_date():
    future_date = date.today() + timedelta(days=1)
    with pytest.raises(HTTPException) as exc_info:
        ensure_date_not_future(future_date)
    assert exc_info.value.status_code == 400
    assert "будущие дни" in exc_info.value.detail

def test_ensure_datetime_not_future_with_past_datetime():
    past_dt = datetime.now() - timedelta(days=1)
    ensure_datetime_not_future(past_dt)

def test_ensure_datetime_not_future_with_future_datetime():
    future_dt = datetime.now() + timedelta(days=1)
    with pytest.raises(HTTPException) as exc_info:
        ensure_datetime_not_future(future_dt)
    assert exc_info.value.status_code == 400
