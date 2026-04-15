from __future__ import annotations

from app.models.analysis_run import AnalysisRun
from app.models.saved_recommendation import SavedRecommendation


def get_trend(delta: int) -> str:
    if delta > 0:
        return "improved"
    if delta < 0:
        return "declined"
    return "unchanged"


def build_score_delta(metric: str, previous_value: int, current_value: int) -> dict:
    delta = current_value - previous_value
    return {
        "metric": metric,
        "previous_value": previous_value,
        "current_value": current_value,
        "delta": delta,
        "trend": get_trend(delta),
    }


def determine_overall_trend(deltas: list[dict]) -> str:
    improved = sum(1 for d in deltas if d["delta"] > 0)
    declined = sum(1 for d in deltas if d["delta"] < 0)

    if improved > 0 and declined == 0:
        return "improved"
    if declined > 0 and improved == 0:
        return "declined"
    if improved == 0 and declined == 0:
        return "unchanged"
    return "mixed"


def build_progress_insights(score_deltas: list[dict]) -> list[dict]:
    insights = []

    metric_names = {
        "health_score": "общий health score",
        "sleep_score": "сон",
        "hydration_score": "гидратация",
        "activity_score": "активность",
        "nutrition_score": "питание",
        "state_score": "самочувствие",
    }

    for item in score_deltas:
        metric = item["metric"]
        delta = item["delta"]
        human_name = metric_names.get(metric, metric)

        if delta >= 8:
            insights.append({
                "metric": metric,
                "title": f"Улучшение: {human_name}",
                "description": f"Показатель «{human_name}» вырос на {delta} пунктов.",
                "impact": "positive"
            })
        elif delta <= -8:
            insights.append({
                "metric": metric,
                "title": f"Ухудшение: {human_name}",
                "description": f"Показатель «{human_name}» снизился на {abs(delta)} пунктов.",
                "impact": "negative"
            })
        elif delta != 0:
            impact = "positive" if delta > 0 else "negative"
            verb = "вырос" if delta > 0 else "снизился"
            insights.append({
                "metric": metric,
                "title": f"Небольшое изменение: {human_name}",
                "description": f"Показатель «{human_name}» {verb} на {abs(delta)} пунктов.",
                "impact": impact
            })

    if not insights:
        insights.append({
            "metric": "overall",
            "title": "Стабильное состояние",
            "description": "По ключевым метрикам существенных изменений не обнаружено.",
            "impact": "neutral"
        })

    return insights


def normalize_recommendation_title(item: SavedRecommendation) -> tuple[str, str]:
    return ((item.title or "").strip().lower(), (item.category or "").strip().lower())


def compare_recommendations(
    previous_items: list[SavedRecommendation],
    current_items: list[SavedRecommendation],
) -> list[dict]:
    previous_recommendations = [
        item for item in previous_items if item.item_type == "recommendation"
    ]
    current_recommendations = [
        item for item in current_items if item.item_type == "recommendation"
    ]

    previous_keys = {normalize_recommendation_title(item): item for item in previous_recommendations}
    current_keys = {normalize_recommendation_title(item): item for item in current_recommendations}

    all_keys = set(previous_keys.keys()) | set(current_keys.keys())

    statuses = []

    for key in sorted(all_keys):
        prev_item = previous_keys.get(key)
        curr_item = current_keys.get(key)

        previous_exists = prev_item is not None
        current_exists = curr_item is not None

        if previous_exists and not current_exists:
            status = "resolved"
            source_item = prev_item
        elif not previous_exists and current_exists:
            status = "new"
            source_item = curr_item
        else:
            status = "persistent"
            source_item = curr_item or prev_item

        statuses.append({
            "title": source_item.title,
            "category": source_item.category,
            "previous_exists": previous_exists,
            "current_exists": current_exists,
            "status": status,
        })

    return statuses


def compare_analysis_runs(
    previous_run: AnalysisRun,
    current_run: AnalysisRun,
    previous_items: list[SavedRecommendation],
    current_items: list[SavedRecommendation],
) -> dict:
    score_deltas = [
        build_score_delta("health_score", previous_run.health_score, current_run.health_score),
        build_score_delta("sleep_score", previous_run.sleep_score, current_run.sleep_score),
        build_score_delta("hydration_score", previous_run.hydration_score, current_run.hydration_score),
        build_score_delta("activity_score", previous_run.activity_score, current_run.activity_score),
        build_score_delta("nutrition_score", previous_run.nutrition_score, current_run.nutrition_score),
        build_score_delta("state_score", previous_run.state_score, current_run.state_score),
    ]

    recommendation_statuses = compare_recommendations(
        previous_items=previous_items,
        current_items=current_items,
    )

    progress_insights = build_progress_insights(score_deltas)

    overall_trend = determine_overall_trend(score_deltas)

    return {
        "summary": {
            "previous_run_id": previous_run.id,
            "current_run_id": current_run.id,
            "previous_created_at": previous_run.created_at,
            "current_created_at": current_run.created_at,
            "overall_trend": overall_trend,
            "health_score_delta": current_run.health_score - previous_run.health_score,
        },
        "score_deltas": score_deltas,
        "progress_insights": progress_insights,
        "recommendation_statuses": recommendation_statuses,
    }