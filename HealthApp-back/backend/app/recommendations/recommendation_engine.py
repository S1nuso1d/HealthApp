from sqlalchemy.orm import Session

from app.models.insight import Insight
from app.recommendations.priority_scoring import PriorityScoring
from app.recommendations.recommendation_builder import RecommendationBuilder
from app.schemas.analytics import RecommendationItem


class RecommendationEngine:
    PRIORITY_ORDER = {
        "high": 3,
        "medium": 2,
        "low": 1,
    }

    @staticmethod
    def generate_recommendations(
        db: Session,
        user_id: int,
    ) -> list[RecommendationItem]:
        insights = (
            db.query(Insight)
            .filter(Insight.user_id == user_id)
            .order_by(Insight.created_at.desc())
            .all()
        )

        recommendations: list[RecommendationItem] = []
        seen_keys: set[tuple[str, str]] = set()

        for insight in insights:
            priority = PriorityScoring.score_priority(insight)
            recommendation = RecommendationBuilder.build_from_insight(insight, priority)

            if recommendation is None:
                continue

            dedupe_key = (recommendation.title, recommendation.related_insight_type or "")
            if dedupe_key in seen_keys:
                continue

            seen_keys.add(dedupe_key)
            recommendations.append(recommendation)

        recommendations.sort(
            key=lambda item: (
                -RecommendationEngine.PRIORITY_ORDER.get(item.priority, 0),
                -(item.confidence or 0.0),
                item.title,
            )
        )

        return recommendations