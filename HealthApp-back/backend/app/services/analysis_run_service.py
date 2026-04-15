from sqlalchemy.orm import Session

from app.models.analysis_run import AnalysisRun


class AnalysisRunService:
    @staticmethod
    def create_run(
        db: Session,
        user_id: int,
        period_days: int,
        summaries_count: int,
        insights_count: int,
        recommendations_count: int,
        smart_triggers_count: int,
        health_score: float | None,
        sleep_score: float | None,
        hydration_score: float | None,
        activity_score: float | None,
        nutrition_score: float | None,
        state_score: float | None,
        status: str = "completed",
    ) -> AnalysisRun:
        run = AnalysisRun(
            user_id=user_id,
            period_days=period_days,
            summaries_count=summaries_count,
            insights_count=insights_count,
            recommendations_count=recommendations_count,
            smart_triggers_count=smart_triggers_count,
            health_score=health_score,
            sleep_score=sleep_score,
            hydration_score=hydration_score,
            activity_score=activity_score,
            nutrition_score=nutrition_score,
            state_score=state_score,
            status=status,
        )
        db.add(run)
        db.commit()
        db.refresh(run)
        return run

    @staticmethod
    def get_runs_for_user(
        db: Session,
        user_id: int,
        limit: int = 20,
    ) -> list[AnalysisRun]:
        return (
            db.query(AnalysisRun)
            .filter(AnalysisRun.user_id == user_id)
            .order_by(AnalysisRun.created_at.desc())
            .limit(limit)
            .all()
        )