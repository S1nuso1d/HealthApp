from app.core.config import settings
from app.llm.llm_client import LLMClient, LLMClientError
from app.llm.prompt_builder import PromptBuilder
from app.llm.response_formatter import ResponseFormatter
from app.schemas.ai import AIBriefResponse, AIResponse
from app.schemas.analytics import AnalyticsResponse


class HealthChatService:
    def __init__(self):
        self.client = LLMClient()

    def _build_fallback_chat_answer(
        self,
        analytics: AnalyticsResponse,
        user_question: str,
    ) -> AIResponse:
        summary = analytics.summary

        weak_areas = []
        if summary.sleep_score < 60:
            weak_areas.append("сон")
        if summary.hydration_score < 60:
            weak_areas.append("гидратация")
        if summary.activity_score < 60:
            weak_areas.append("активность")
        if summary.nutrition_score < 60:
            weak_areas.append("питание")
        if summary.state_score < 60:
            weak_areas.append("субъективное состояние")

        weak_text = ", ".join(weak_areas) if weak_areas else "явно слабых зон сейчас не видно"

        today_note = ""
        if "Контекст на сегодня" in user_question or "контекст на сегодня" in user_question.lower():
            today_note = (
                "\n\n(В вопросе переданы сегодняшние показатели — учти их в первую очередь.)"
            )

        insight_titles = [item.title for item in analytics.insights[:3]]
        insight_text = "; ".join(insight_titles) if insight_titles else "явных инсайтов пока нет"

        recommendation_texts = [
            f"- {item.title}: {item.action or item.description}"
            for item in analytics.recommendations[:3]
        ]

        recommendation_block = "\n".join(recommendation_texts) if recommendation_texts else "- Пока рекомендаций нет"

        text = f"""
Сейчас не удалось связаться с языковой моделью — краткий ответ по вашей аналитике в приложении.

Ваш вопрос: {user_question}{today_note}

По текущей аналитике:
- общий health score: {summary.health_score}/100
- основные слабые зоны: {weak_text}
- главные инсайты: {insight_text}

Что можно сделать прямо сейчас:
{recommendation_block}
        """.strip()

        return ResponseFormatter.format_chat_response(text, source="fallback")

    def _build_fallback_daily_brief(
        self,
        analytics: AnalyticsResponse,
    ) -> AIBriefResponse:
        summary = analytics.summary

        key_points = [
            f"Health score: {summary.health_score}/100",
            f"Sleep: {summary.sleep_score}/100, Hydration: {summary.hydration_score}/100",
            f"Activity: {summary.activity_score}/100, Nutrition: {summary.nutrition_score}/100",
        ]

        if analytics.insights:
            key_points.append(f"Главный инсайт: {analytics.insights[0].title}")

        if analytics.recommendations:
            key_points.append(f"Главное действие: {analytics.recommendations[0].action or analytics.recommendations[0].title}")

        summary_text = (
            "Это fallback daily brief на основе уже рассчитанной аналитики. "
            "Он показывает текущее состояние и главный вектор улучшения."
        )

        return AIBriefResponse(
            title="Ежедневный health brief",
            summary=summary_text,
            key_points=key_points[:5],
            generated_at=ResponseFormatter.format_chat_response("x").generated_at,
            source="fallback",
        )

    def _build_fallback_weekly_brief(
        self,
        analytics: AnalyticsResponse,
    ) -> AIBriefResponse:
        summary = analytics.summary

        strongest = min(
            [
                ("сон", summary.sleep_score),
                ("гидратация", summary.hydration_score),
                ("активность", summary.activity_score),
                ("питание", summary.nutrition_score),
                ("состояние", summary.state_score),
            ],
            key=lambda x: x[1]
        )

        key_points = [
            f"Общий health score: {summary.health_score}/100",
            f"Основная слабая зона недели: {strongest[0]} ({strongest[1]}/100)",
            f"Инсайтов найдено: {len(analytics.insights)}",
            f"Рекомендаций сформировано: {len(analytics.recommendations)}",
        ]

        return AIBriefResponse(
            title="Еженедельный health brief",
            summary=(
                "Это fallback weekly brief. Он показывает общий результат периода и "
                "наиболее важное направление улучшения."
            ),
            key_points=key_points,
            generated_at=ResponseFormatter.format_chat_response("x").generated_at,
            source="fallback",
        )

    def _build_fallback_explain_insight(
        self,
        analytics: AnalyticsResponse,
        insight_title: str,
    ) -> AIResponse:
        matched = next((item for item in analytics.insights if item.title == insight_title), None)

        if matched is None:
            return ResponseFormatter.format_chat_response(
                "Я не нашел этот инсайт в текущем аналитическом контексте.",
                source="fallback",
            )

        evidence_lines = []
        for ev in matched.evidence[:3]:
            unit = f" {ev.unit}" if ev.unit else ""
            note = f" ({ev.note})" if ev.note else ""
            evidence_lines.append(f"- {ev.metric}: {ev.value}{unit}{note}")

        evidence_block = "\n".join(evidence_lines) if evidence_lines else "- Дополнительные доказательства пока не сохранены"

        text = f"""
Инсайт: {matched.title}

Что это значит:
{matched.description}

Почему система могла к этому прийти:
{evidence_block}

Практический смысл:
Этот инсайт указывает на паттерн, который может влиять на твое самочувствие или режим.

Что можно попробовать:
Сфокусируйся на ближайшей рекомендации, связанной с этим инсайтом, и посмотри, как изменятся показатели в течение нескольких дней.
        """.strip()

        return ResponseFormatter.format_chat_response(text, source="fallback")

    def generate_chat_answer(
        self,
        analytics: AnalyticsResponse,
        user_question: str,
        today: dict | None = None,
        personal_hints: list[dict] | None = None,
    ) -> AIResponse:
        prompt = PromptBuilder.build_chat_prompt(
            analytics=analytics,
            user_question=user_question,
            today=today,
            personal_hints=personal_hints,
        )

        try:
            raw = self.client.generate(
                prompt=prompt,
                system_prompt=PromptBuilder.SYSTEM_PROMPT,
                temperature=0.35,
            )
            return ResponseFormatter.format_chat_response(raw, source="llm")
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return self._build_fallback_chat_answer(analytics, user_question)
            raise

    def generate_daily_brief(
        self,
        analytics: AnalyticsResponse,
    ) -> AIBriefResponse:
        prompt = PromptBuilder.build_daily_brief_prompt(analytics=analytics)

        try:
            raw = self.client.generate(
                prompt=prompt,
                system_prompt=PromptBuilder.SYSTEM_PROMPT,
                temperature=0.3,
            )
            return ResponseFormatter.format_brief_response(
                raw_text=raw,
                default_title="Ежедневный health brief",
                source="llm",
            )
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return self._build_fallback_daily_brief(analytics)
            raise

    def generate_weekly_brief(
        self,
        analytics: AnalyticsResponse,
    ) -> AIBriefResponse:
        prompt = PromptBuilder.build_weekly_brief_prompt(analytics=analytics)

        try:
            raw = self.client.generate(
                prompt=prompt,
                system_prompt=PromptBuilder.SYSTEM_PROMPT,
                temperature=0.3,
            )
            return ResponseFormatter.format_brief_response(
                raw_text=raw,
                default_title="Еженедельный health brief",
                source="llm",
            )
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return self._build_fallback_weekly_brief(analytics)
            raise

    def explain_insight(
        self,
        analytics: AnalyticsResponse,
        insight_title: str,
    ) -> AIResponse:
        prompt = PromptBuilder.build_explain_insight_prompt(
            analytics=analytics,
            insight_title=insight_title,
        )

        try:
            raw = self.client.generate(
                prompt=prompt,
                system_prompt=PromptBuilder.SYSTEM_PROMPT,
                temperature=0.25,
            )
            return ResponseFormatter.format_chat_response(raw, source="llm")
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return self._build_fallback_explain_insight(analytics, insight_title)
            raise