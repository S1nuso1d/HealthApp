"""Слияние аналитических и персональных рекомендаций + AI-обогащение и авто-закрытие по целям."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone

from sqlalchemy.orm import Session

from app.llm.llm_client import LLMClient, LLMClientError
from app.models.saved_recommendation import SavedRecommendation
from app.recommendations.recommendation_engine import RecommendationEngine
from app.schemas.ai import AIRecommendationItem
from app.schemas.analytics import AnalyticsResponse, RecommendationItem
from app.services.action_plan_sync_service import TodayGoalsSnapshot, collect_today_goals
from app.services.personalized_advisor import PersonalizedAdvisor


def merge_recommendation_lists(
    personalized: list[dict],
    from_analytics: list[RecommendationItem],
) -> list[RecommendationItem]:
    merged: list[RecommendationItem] = []
    seen: set[str] = set()
    for raw in personalized:
        title = raw["title"]
        if title in seen:
            continue
        seen.add(title)
        merged.append(
            RecommendationItem(
                category=raw["category"],
                title=title,
                description=raw["description"],
                priority=raw["priority"],
                confidence=raw.get("confidence", 0.8),
                action=raw.get("action"),
                related_insight_title=None,
                related_insight_type=raw.get("why_this"),
            )
        )
    for item in from_analytics:
        if item.title not in seen:
            seen.add(item.title)
            merged.append(item)
    return merged


def build_merged_recommendation_items(
    db: Session,
    user_id: int,
    days: int,
) -> list[RecommendationItem]:
    engine_items = RecommendationEngine.generate_recommendations(db=db, user_id=user_id)
    personal_raw = PersonalizedAdvisor.generate_recommendations(
        db=db,
        user_id=user_id,
        period_days=days,
    )
    return merge_recommendation_lists(personal_raw, engine_items)


def _build_personalized_tip_with_llm(
    recommendation: AIRecommendationItem,
    metrics_context: dict[str, int],
) -> str | None:
    prompt = f"""
Сформируй короткий персональный совет на русском языке (1-2 предложения, без диагнозов).

Рекомендация:
- category: {recommendation.category}
- title: {recommendation.title}
- description: {recommendation.description}
- action: {recommendation.action or "нет"}

Контекст пользователя:
- health_score: {metrics_context["health_score"]}
- hydration_score: {metrics_context["hydration_score"]}
- sleep_score: {metrics_context["sleep_score"]}
- activity_score: {metrics_context["activity_score"]}
- nutrition_score: {metrics_context["nutrition_score"]}
- state_score: {metrics_context["state_score"]}

Ответ только текстом совета, без списков и markdown.
    """.strip()
    try:
        return LLMClient().generate(prompt=prompt, temperature=0.25)
    except LLMClientError:
        return None


def _build_personalized_tip_fast(recommendation: AIRecommendationItem) -> str | None:
    if recommendation.action and recommendation.description:
        desc = recommendation.description.strip()
        if len(desc) > 160:
            return f"{desc[:160]}… {recommendation.action}"
        return f"{desc} {recommendation.action}"
    if recommendation.action:
        return recommendation.action
    return recommendation.description


def _apply_goal_resolution(
    rec: AIRecommendationItem,
    goals: TodayGoalsSnapshot,
) -> str | None:
    """Помечает рекомендацию resolved, если дневная цель достигнута. Возвращает причину."""
    category = (rec.category or "").lower()

    if category == "hydration":
        rec.progress_label = f"Сегодня: {goals.water_ml}/{goals.water_target} мл"
        if goals.water_ml >= goals.water_target:
            rec.status = "resolved"
            return "Цель по воде на сегодня достигнута."
        remaining = max(goals.water_target - goals.water_ml, 0)
        rec.action = (
            f"До дневной цели осталось около {remaining} мл. "
            "Добавьте 1–2 приёма воды в ближайшие часы."
        )
        return None

    if category == "sleep":
        rec.progress_label = f"Сон сегодня: {goals.sleep_hours:.1f}/{goals.sleep_target:.1f} ч"
        if goals.sleep_hours >= goals.sleep_target:
            rec.status = "resolved"
            return "Сон сегодня соответствует вашей цели."
        remaining = max(goals.sleep_target - goals.sleep_hours, 0.0)
        rec.action = (
            f"До цели по сну не хватает ~{remaining:.1f} ч. "
            "Сместите отбой на 30–60 минут раньше."
        )
        return None

    if category == "activity":
        rec.progress_label = f"Сегодня: {goals.steps}/{goals.steps_target} шагов"
        if goals.steps >= goals.steps_target:
            rec.status = "resolved"
            return "Дневная цель по шагам достигнута."
        remaining = max(goals.steps_target - goals.steps, 0)
        rec.action = (
            f"Осталось примерно {remaining} шагов. "
            "Добавьте 1–2 короткие прогулки."
        )
        return None

    if category in {"meals", "nutrition"}:
        rec.progress_label = f"Калории: {goals.calories}/{goals.calories_target} ккал"
        if goals.calories >= int(goals.calories_target * 0.98):
            rec.status = "resolved"
            return "Дневная цель по калориям достигнута."
        remaining = max(goals.calories_target - goals.calories, 0)
        rec.action = (
            f"До цели по калориям осталось ~{remaining} ккал. "
            "Сбалансируйте оставшиеся приёмы пищи."
        )
        return None

    if category == "state":
        if goals.state_logged_today:
            rec.status = "resolved"
            return "Субъективное состояние уже отмечено сегодня."
        rec.action = rec.action or "Отметьте настроение и энергию в дневнике — это займёт меньше минуты."
        return None

    return None


def build_dynamic_ai_recommendations(
    db: Session,
    user_id: int,
    analytics: AnalyticsResponse,
    recommendation_items: list[RecommendationItem],
    include_resolved: bool = False,
    use_llm_tips: bool = False,
) -> list[AIRecommendationItem]:
    goals = collect_today_goals(db, user_id)

    metrics_context = {
        "health_score": analytics.summary.health_score,
        "hydration_score": analytics.summary.hydration_score,
        "sleep_score": analytics.summary.sleep_score,
        "activity_score": analytics.summary.activity_score,
        "nutrition_score": analytics.summary.nutrition_score,
        "state_score": analytics.summary.state_score,
    }

    existing_items = (
        db.query(SavedRecommendation)
        .filter(SavedRecommendation.user_id == user_id)
        .all()
    )
    saved_by_key = {(entry.category, entry.title): entry for entry in existing_items}
    now_utc = datetime.now(timezone.utc)
    cooldown_duration = timedelta(hours=12)

    items: list[AIRecommendationItem] = []
    for item in recommendation_items:
        rec_key = (item.category, item.title)
        saved = saved_by_key.get(rec_key)

        rec = AIRecommendationItem(
            category=item.category,
            title=item.title,
            description=item.description,
            priority=item.priority,
            status="active",
            confidence=item.confidence,
            action=item.action,
            related_insight_title=item.related_insight_title,
            related_insight_type=item.related_insight_type,
        )

        resolved_reason = _apply_goal_resolution(rec, goals)

        if rec.status == "active" and saved and saved.status == "resolved":
            if saved.created_at:
                saved_created_at = saved.created_at
                if saved_created_at.tzinfo is None:
                    saved_created_at = saved_created_at.replace(tzinfo=timezone.utc)
                if now_utc < (saved_created_at + cooldown_duration):
                    rec.status = "cooldown"
                    rec.progress_label = rec.progress_label or "Совет скрыт после выполнения цели."

        if rec.status in {"resolved", "cooldown"}:
            if saved is None:
                saved = SavedRecommendation(
                    user_id=user_id,
                    analysis_run_id=None,
                    category=rec.category,
                    title=rec.title,
                    description=rec.description,
                    priority=rec.priority,
                    confidence=rec.confidence,
                    action=resolved_reason,
                    related_insight_type=rec.related_insight_type,
                    related_insight_title=rec.related_insight_title,
                    status="resolved" if rec.status == "resolved" else "cooldown",
                )
                db.add(saved)
            else:
                saved.status = "resolved" if rec.status == "resolved" else "cooldown"
                saved.action = resolved_reason or saved.action
                saved.description = rec.description
                saved.priority = rec.priority
                saved.confidence = rec.confidence

            if include_resolved:
                rec.personalized_tip = "Отличная работа! Эта рекомендация уже выполнена."
                items.append(rec)
            continue

        if saved:
            saved.status = "active"
            saved.description = rec.description
            saved.priority = rec.priority
            saved.confidence = rec.confidence
            saved.action = rec.action
        else:
            saved = SavedRecommendation(
                user_id=user_id,
                analysis_run_id=None,
                category=rec.category,
                title=rec.title,
                description=rec.description,
                priority=rec.priority,
                confidence=rec.confidence,
                action=rec.action,
                related_insight_type=rec.related_insight_type,
                related_insight_title=rec.related_insight_title,
                status="active",
            )
            db.add(saved)

        rec.personalized_tip = (
            _build_personalized_tip_with_llm(rec, metrics_context)
            if use_llm_tips
            else _build_personalized_tip_fast(rec)
        )
        items.append(rec)

    db.commit()
    return items
