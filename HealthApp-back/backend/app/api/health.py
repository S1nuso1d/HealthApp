"""Показатели здоровья (выборки из импорта / Health Connect)."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.health_sample import HealthSample
from app.models.user import User
from app.schemas.health_sample import HealthSampleOut

router = APIRouter(prefix="/health", tags=["Health"])


@router.get("/samples", response_model=list[HealthSampleOut])
def list_health_samples(
    days: int = Query(14, ge=1, le=365),
    metrics: str | None = Query(
        None,
        description="Список кодов metric через запятую; если не задан — все",
    ),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    since = datetime.now(timezone.utc) - timedelta(days=days)
    q = (
        db.query(HealthSample)
        .filter(
            HealthSample.user_id == current_user.id,
            HealthSample.recorded_at >= since,
        )
    )
    if metrics and metrics.strip():
        mset = {x.strip() for x in metrics.split(",") if x.strip()}
        if mset:
            q = q.filter(HealthSample.metric.in_(mset))
    rows = q.order_by(HealthSample.recorded_at.asc()).all()
    return rows
