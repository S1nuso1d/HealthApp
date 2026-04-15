from __future__ import annotations

from app.models.action_plan import ActionPlan


def build_action_plan_from_recommendations(
    analysis_run_id: int,
    user_id: int,
    recommendations: list[dict],
) -> list[ActionPlan]:
    plans: list[ActionPlan] = []

    for item in recommendations:
        category = item.get("category", "")
        priority = item.get("priority", "medium")
        title = item.get("title", "")
        description = item.get("description", "")
        action = item.get("action")

        plan_type = "week"

        if category in {"sleep", "state"}:
            plan_type = "evening"
        elif category in {"hydration", "activity"}:
            plan_type = "today"
        elif category in {"meals", "correlation"}:
            plan_type = "week"

        plans.append(
            ActionPlan(
                analysis_run_id=analysis_run_id,
                user_id=user_id,
                plan_type=plan_type,
                title=title,
                description=description,
                priority=priority,
                status="active",
                action=action,
            )
        )

    return plans