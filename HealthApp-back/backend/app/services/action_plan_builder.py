from sqlalchemy.orm import Session

from app.models.action_plan import ActionPlan
from app.recommendations.recommendation_engine import RecommendationEngine


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
    ) -> list[ActionPlan]:
        """
        Генерирует action plan из рекомендаций пользователя.
        """

        if replace_existing:
            ActionPlanBuilder.clear_existing_action_plans(db, user_id)

        recommendations = RecommendationEngine.generate_recommendations(
            db=db,
            user_id=user_id,
        )

        selected = recommendations[:limit]
        created_items: list[ActionPlan] = []

        for rec in selected:
            action_plan = ActionPlan(
                user_id=user_id,
                category=rec.category,
                title=rec.title,
                description=rec.description,
                priority=rec.priority,
                status="pending",
                action_text=rec.action,
                source_insight_type=rec.related_insight_type,
                source_insight_title=rec.related_insight_title,
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