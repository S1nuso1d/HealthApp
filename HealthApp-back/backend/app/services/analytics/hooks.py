import logging
from datetime import date

from sqlalchemy.orm import Session

from app.services.analytics.daily_summary_service import DailySummaryService
from app.services.analytics.insight_service import InsightService

logger = logging.getLogger(__name__)


def safe_rebuild_user_analytics_for_date(
    db: Session,
    user_id: int,
    target_date: date,
    window_days: int = 7,
) -> None:
    """
    Безопасно пересчитывает дневную сводку и инсайты пользователя.
    Ошибки аналитики не должны ломать основной CRUD.
    """
    try:
        DailySummaryService.rebuild_summary_for_day(
            db=db,
            user_id=user_id,
            target_date=target_date,
        )
        InsightService.rebuild_insights_for_user(
            db=db,
            user_id=user_id,
            window_days=window_days,
        )
    except Exception:
        logger.exception(
            "Не удалось пересчитать аналитику для user_id=%s, target_date=%s",
            user_id,
            target_date,
        )