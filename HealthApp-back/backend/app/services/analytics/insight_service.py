import json

from sqlalchemy.orm import Session

from app.models.daily_health_summary import DailyHealthSummary
from app.models.insight import Insight
from app.services.correlation_analyzer import CorrelationAnalyzer
from app.services.analytics.user_trends_service import build_trend_insight_candidates, compute_user_trends
from app.services.personalized_advisor import PersonalizedAdvisor


def build_evidence(metric: str, value: float, unit: str | None = None, note: str | None = None) -> dict:
    return {
        "metric": metric,
        "value": round(float(value), 2),
        "unit": unit,
        "note": note,
    }


class InsightService:
    @staticmethod
    def rebuild_insights_for_user(
        db: Session,
        user_id: int,
        window_days: int = 7
    ) -> list[Insight]:
        """
        Полностью пересобирает инсайты пользователя:
        1. удаляет старые инсайты
        2. строит базовые инсайты по daily summaries
        3. строит correlation-инсайты
        4. сохраняет всё в таблицу insights
        """

        # Удаляем старые инсайты пользователя
        db.query(Insight).filter(Insight.user_id == user_id).delete()
        db.commit()

        summaries = (
            db.query(DailyHealthSummary)
            .filter(DailyHealthSummary.user_id == user_id)
            .order_by(DailyHealthSummary.summary_date.desc())
            .limit(window_days)
            .all()
        )

        summaries = list(reversed(summaries))

        created_insights: list[Insight] = []

        # ------------------------------------------------------------
        # 1. БАЗОВЫЕ ИНСАЙТЫ ПО DAILY SUMMARY
        # ------------------------------------------------------------
        if summaries:
            # -------- Инсайт: недосып --------
            sleep_values = [s.total_sleep_hours for s in summaries if s.total_sleep_hours is not None]
            if sleep_values:
                avg_sleep = round(sum(sleep_values) / len(sleep_values), 2)
                if avg_sleep < 7:
                    insight = Insight(
                        user_id=user_id,
                        insight_type="low_sleep_duration",
                        category="sleep",
                        title="Недостаточная длительность сна",
                        description=(
                            f"За последние {window_days} дней средняя длительность сна "
                            f"составила {avg_sleep} ч. Это ниже рекомендуемого уровня."
                        ),
                        confidence=0.82,
                        severity="high" if avg_sleep < 6 else "medium",
                        impact="negative",
                        evidence_json=json.dumps(
                            [
                                build_evidence(
                                    "average_sleep_hours",
                                    avg_sleep,
                                    "hours",
                                    f"Среднее значение за {window_days} дней",
                                )
                            ],
                            ensure_ascii=False,
                        ),
                        window_days=window_days,
                    )
                    db.add(insight)
                    created_insights.append(insight)

            # -------- Инсайт: недобор воды --------
            water_values = [s.total_water_ml for s in summaries if s.total_water_ml is not None]
            if water_values:
                avg_water = round(sum(water_values) / len(water_values), 2)
                if avg_water < 2000:
                    insight = Insight(
                        user_id=user_id,
                        insight_type="low_hydration",
                        category="hydration",
                        title="Недостаточное потребление воды",
                        description=(
                            f"За последние {window_days} дней среднее потребление воды "
                            f"составило {int(avg_water)} мл в день. Это может быть ниже оптимального уровня."
                        ),
                        confidence=0.76,
                        severity="medium",
                        impact="negative",
                        evidence_json=json.dumps(
                            [
                                build_evidence(
                                    "average_water_ml",
                                    avg_water,
                                    "ml",
                                    f"Среднее значение за {window_days} дней",
                                )
                            ],
                            ensure_ascii=False,
                        ),
                        window_days=window_days,
                    )
                    db.add(insight)
                    created_insights.append(insight)

            # -------- Инсайт: высокий кофеин --------
            caffeine_values = [s.total_caffeine_mg for s in summaries if s.total_caffeine_mg is not None]
            if caffeine_values:
                avg_caffeine = round(sum(caffeine_values) / len(caffeine_values), 2)
                if avg_caffeine > 250:
                    insight = Insight(
                        user_id=user_id,
                        insight_type="high_caffeine_intake",
                        category="meals",
                        title="Повышенное потребление кофеина",
                        description=(
                            f"За последние {window_days} дней среднее потребление кофеина "
                            f"составило {avg_caffeine} мг в день. Это может ухудшать качество сна и восстановление."
                        ),
                        confidence=0.71,
                        severity="medium",
                        impact="negative",
                        evidence_json=json.dumps(
                            [
                                build_evidence(
                                    "average_caffeine_mg",
                                    avg_caffeine,
                                    "mg",
                                    f"Среднее значение за {window_days} дней",
                                )
                            ],
                            ensure_ascii=False,
                        ),
                        window_days=window_days,
                    )
                    db.add(insight)
                    created_insights.append(insight)

            # -------- Инсайт: низкая активность --------
            step_values = [s.total_steps for s in summaries if s.total_steps is not None]
            if step_values:
                avg_steps = round(sum(step_values) / len(step_values), 2)
                if avg_steps < 7000:
                    insight = Insight(
                        user_id=user_id,
                        insight_type="low_daily_activity",
                        category="activity",
                        title="Низкий уровень ежедневной активности",
                        description=(
                            f"За последние {window_days} дней среднее количество шагов "
                            f"составило {int(avg_steps)} в день. Это ниже желаемого уровня повседневной активности."
                        ),
                        confidence=0.74,
                        severity="medium",
                        impact="negative",
                        evidence_json=json.dumps(
                            [
                                build_evidence(
                                    "average_steps",
                                    avg_steps,
                                    "steps",
                                    f"Среднее значение за {window_days} дней",
                                )
                            ],
                            ensure_ascii=False,
                        ),
                        window_days=window_days,
                    )
                    db.add(insight)
                    created_insights.append(insight)

            # -------- Инсайт: низкий субъективный state score --------
            state_values = [s.total_state_score for s in summaries if s.total_state_score is not None]
            if state_values:
                avg_state = round(sum(state_values) / len(state_values), 2)
                if avg_state < 5.5:
                    insight = Insight(
                        user_id=user_id,
                        insight_type="low_subjective_state",
                        category="state",
                        title="Сниженное субъективное состояние",
                        description=(
                            f"За последние {window_days} дней средняя субъективная оценка состояния "
                            f"составила {avg_state} из 10. Это может указывать на накопленную усталость или дисбаланс режима."
                        ),
                        confidence=0.69,
                        severity="medium",
                        impact="negative",
                        evidence_json=json.dumps(
                            [
                                build_evidence(
                                    "average_state_score",
                                    avg_state,
                                    "score_1_10",
                                    f"Среднее значение за {window_days} дней",
                                )
                            ],
                            ensure_ascii=False,
                        ),
                        window_days=window_days,
                    )
                    db.add(insight)
                    created_insights.append(insight)

        # ------------------------------------------------------------
        # 2. CORRELATION-ИНСАЙТЫ
        # ------------------------------------------------------------
        correlation_insights = CorrelationAnalyzer.analyze_correlations(
            db=db,
            user_id=user_id,
            period_days=window_days,
        )

        for item in correlation_insights:
            insight = Insight(
                user_id=user_id,
                insight_type=item["insight_type"],
                category=item.get("category", "correlation"),
                title=item["title"],
                description=item["description"],
                confidence=item.get("confidence", 0.6),
                severity=item.get("severity", "medium"),
                impact=item.get("impact", "neutral"),
                evidence_json=item.get("evidence_json"),
                window_days=item.get("window_days", window_days),
            )
            db.add(insight)
            created_insights.append(insight)

        trends = compute_user_trends(db, user_id, days=window_days)
        existing_titles = {i.title for i in created_insights}
        for item in build_trend_insight_candidates(trends):
            if item["title"] in existing_titles:
                continue
            existing_titles.add(item["title"])
            insight = Insight(
                user_id=user_id,
                insight_type=item["insight_type"],
                category=item.get("category", "correlation"),
                title=item["title"],
                description=item["description"],
                confidence=item.get("confidence", 0.7),
                severity=item.get("severity", "medium"),
                impact=item.get("impact", "neutral"),
                evidence_json=None,
                window_days=window_days,
            )
            db.add(insight)
            created_insights.append(insight)

        for item in PersonalizedAdvisor.generate_insights(db, user_id, window_days):
            if item["title"] in existing_titles:
                continue
            existing_titles.add(item["title"])
            insight = Insight(
                user_id=user_id,
                insight_type=item["insight_type"],
                category=item.get("category", "meals"),
                title=item["title"],
                description=item["description"],
                confidence=item.get("confidence", 0.8),
                severity=item.get("severity", "high"),
                impact=item.get("impact", "negative"),
                evidence_json=None,
                window_days=item.get("window_days", window_days),
            )
            db.add(insight)
            created_insights.append(insight)

        db.commit()

        for insight in created_insights:
            db.refresh(insight)

        return created_insights