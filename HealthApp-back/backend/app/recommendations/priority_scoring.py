from app.models.insight import Insight


class PriorityScoring:
    @staticmethod
    def score_priority(insight: Insight) -> str:
        """
        Возвращает приоритет рекомендации на основе:
        - severity инсайта
        - confidence
        - impact
        """

        severity = (insight.severity or "low").lower()
        impact = (insight.impact or "neutral").lower()
        confidence = float(insight.confidence or 0.0)

        # Жесткие правила высокого приоритета
        if severity == "high" and confidence >= 0.75:
            return "high"

        if severity == "high" and impact == "negative":
            return "high"

        if severity == "medium" and confidence >= 0.85 and impact == "negative":
            return "high"

        # Средний приоритет
        if severity == "medium":
            return "medium"

        if severity == "low" and confidence >= 0.8 and impact == "negative":
            return "medium"

        # Позитивные или слабые сигналы
        return "low"