from sqlalchemy.orm import Session

from app.models.action_plan import ActionPlan
from app.services.action_plan_sync_service import (
    select_personalized_recommendations,
)


class ActionPlanBuilder:
    @staticmethod
    def clear_existing_action_plans(db: Session, user_id: int) -> None:
        db.query(ActionPlan).filter(ActionPlan.user_id == user_id).delete()
        db.commit()

    @staticmethod
    def generate_for_user(
        db: Session,
        user_id: int,
        limit: int = 5,
        replace_existing: bool = True,
        use_personalized: bool = True,
    ) -> list[ActionPlan]:
        """
        Генерирует action plan: персональные рекомендации с ротацией по дню
        или классический список из инсайтов.
        """

        if replace_existing:
            ActionPlanBuilder.clear_existing_action_plans(db, user_id)

        if use_personalized:
            selected = select_personalized_recommendations(db, user_id, limit=limit)
        else:
            from app.recommendations.recommendation_engine import RecommendationEngine

            selected = [
                {
                    "category": r.category,
                    "title": r.title,
                    "description": r.description,
                    "priority": r.priority,
                    "action": r.action,
                    "related_insight_type": r.related_insight_type,
                    "related_insight_title": r.related_insight_title,
                }
                for r in RecommendationEngine.generate_recommendations(db=db, user_id=user_id)[:limit]
            ]

        created_items: list[ActionPlan] = []

        for rec in selected:
            action_plan = ActionPlan(
                user_id=user_id,
                category=rec.get("category", "correlation"),
                title=rec.get("title", "Задача"),
                description=rec.get("description", ""),
                priority=rec.get("priority", "medium"),
                status="pending",
                action_text=rec.get("action"),
                source_insight_type=rec.get("related_insight_type"),
                source_insight_title=rec.get("related_insight_title"),
            )
            db.add(action_plan)
            created_items.append(action_plan)

        db.commit()

        for item in created_items:
            db.refresh(item)

        return created_items

    @staticmethod
    def get_for_user(db: Session, user_id: int) -> list[ActionPlan]:
        return (
            db.query(ActionPlan)
            .filter(ActionPlan.user_id == user_id)
            .order_by(ActionPlan.created_at.desc())
            .all()
        )

    @staticmethod
    def get_by_id(db: Session, user_id: int, action_plan_id: int) -> ActionPlan | None:
        return (
            db.query(ActionPlan)
            .filter(
                ActionPlan.id == action_plan_id,
                ActionPlan.user_id == user_id,
            )
            .first()
        )

    @staticmethod
    def update_status(
        db: Session,
        user_id: int,
        action_plan_id: int,
        status: str,
    ) -> ActionPlan | None:
        item = (
            db.query(ActionPlan)
            .filter(
                ActionPlan.id == action_plan_id,
                ActionPlan.user_id == user_id,
            )
            .first()
        )

        if item is None:
            return None

        item.status = status
        db.commit()
        db.refresh(item)
        return item

    @staticmethod
    def delete_item(
        db: Session,
        user_id: int,
        action_plan_id: int,
    ) -> bool:
        item = (
            db.query(ActionPlan)
            .filter(
                ActionPlan.id == action_plan_id,
                ActionPlan.user_id == user_id,
            )
            .first()
        )

        if item is None:
            return False

        db.delete(item)
        db.commit()
        return True