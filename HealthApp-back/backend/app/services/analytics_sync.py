from sqlalchemy.orm import Session

from app.services.analytics.daily_summary_service import DailySummaryService
from app.services.analytics.insight_service import InsightService


def rebuild_user_analytics(db: Session, user_id: int, days: int = 7) -> None:
    """
    Пересчитывает дневные сводки и инсайты пользователя после изменения данных.
    """
    DailySummaryService.rebuild_summaries_for_last_days(
        db=db,
        user_id=user_id,
        days=days,
    )

    InsightService.rebuild_insights_for_user(
        db=db,
        user_id=user_id,
        window_days=days,
    )