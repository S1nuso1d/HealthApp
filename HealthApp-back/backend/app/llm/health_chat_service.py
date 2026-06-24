from app.core.config import settings
from app.llm.text_sanitizer import sanitize_llm_markdown
import typing
from app.llm.llm_client import LLMClient, LLMClientError
from app.llm.prompt_builder import PromptBuilder
from app.llm.response_formatter import ResponseFormatter
from app.schemas.ai import AIBriefResponse, AIResponse
from app.schemas.analytics import AnalyticsResponse


class HealthChatService:
    def __init__(self):
        self.client = LLMClient()
        self.meal_plan_client = LLMClient(model_name=settings.LLM_MEAL_PLAN_MODEL)

    def _build_fallback_chat_answer(
        self,
        analytics: AnalyticsResponse,
        user_question: str,
        user_health_context: str | None = None,
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

        context_note = ""
        if user_health_context:
            excerpt = user_health_context[:1200]
            if len(user_health_context) > 1200:
                excerpt += "…"
            context_note = f"\n\nФрагмент вашего дневника:\n{excerpt}"

        provider_label = "OpenAI" if settings.LLM_PROVIDER == "openai" else "Ollama"
        text = f"""
Сейчас не удалось связаться с языковой моделью ({provider_label}). Краткий ответ по данным HealthApp.

Ваш вопрос: {user_question}{today_note}

По текущей аналитике:
- общий индекс: {summary.health_score}/100
- слабые зоны: {weak_text}
- инсайты: {insight_text}

Рекомендации приложения:
{recommendation_block}
{context_note}

Проверьте настройки LLM в .env на сервере (LLM_PROVIDER, LLM_BASE_URL, LLM_MODEL_NAME).
Для Ollama: запустите `ollama serve` и загрузите модель `ollama pull {settings.LLM_MODEL_NAME}`.
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
        user_health_context: str | None = None,
        history: list[dict[str, str]] | None = None,
        dietary_rules: str | None = None,
    ) -> AIResponse:
        try:
            if user_health_context:
                messages = PromptBuilder.build_chat_messages(
                    user_health_context=user_health_context,
                    user_question=user_question,
                    history=history,
                    dietary_rules=dietary_rules,
                )
                raw = self.client.chat(messages=messages, temperature=0.4)
            else:
                prompt = PromptBuilder.build_chat_prompt(
                    analytics=analytics,
                    user_question=user_question,
                    today=today,
                    personal_hints=personal_hints,
                )
                raw = self.client.generate(
                    prompt=prompt,
                    system_prompt=PromptBuilder.SYSTEM_PROMPT,
                    temperature=0.35,
                )
            return ResponseFormatter.format_chat_response(raw, source="llm")
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return self._build_fallback_chat_answer(
                    analytics,
                    user_question,
                    user_health_context=user_health_context,
                )
            raise

    def generate_chat_stream(
        self,
        analytics: AnalyticsResponse,
        user_question: str,
        today: dict | None = None,
        personal_hints: list[dict] | None = None,
        user_health_context: str | None = None,
        history: list[dict[str, str]] | None = None,
        dietary_rules: str | None = None,
    ) -> typing.Any:
        try:
            if user_health_context:
                messages = PromptBuilder.build_chat_messages(
                    user_health_context=user_health_context,
                    user_question=user_question,
                    history=history,
                    dietary_rules=dietary_rules,
                )
                return self.client.chat(messages=messages, temperature=0.4, stream=True)
            else:
                prompt = PromptBuilder.build_chat_prompt(
                    analytics=analytics,
                    user_question=user_question,
                    today=today,
                    personal_hints=personal_hints,
                )
                messages = []
                if PromptBuilder.SYSTEM_PROMPT:
                    messages.append({"role": "system", "content": PromptBuilder.SYSTEM_PROMPT.strip()})
                messages.append({"role": "user", "content": prompt.strip()})
                return self.client.chat(messages=messages, temperature=0.35, stream=True)
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                fallback_resp = self._build_fallback_chat_answer(
                    analytics,
                    user_question,
                    user_health_context=user_health_context,
                )
                def generate():
                    yield fallback_resp.answer
                return generate()
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

    def _build_fallback_dashboard_hints(self, analytics: AnalyticsResponse) -> str:
        import json

        from app.services.ai.time_context import time_aware_hint

        summary = analytics.summary
        hints: list[str] = []

        time_hint = time_aware_hint()
        if time_hint:
            hints.append(time_hint)
        if summary.hydration_score < 65:
            hints.append("Сегодня мало воды — выпейте стакан прямо сейчас.")
        if summary.sleep_score < 65:
            hints.append("Сон был коротким — постарайтесь лечь раньше сегодня.")
        if summary.activity_score < 65:
            hints.append("Добавьте короткую прогулку, чтобы набрать шаги.")
        if summary.nutrition_score < 65:
            hints.append("Проверьте КБЖУ — возможно, не хватает белка или овощей.")
        if not hints and analytics.recommendations:
            hints.append(analytics.recommendations[0].action or analytics.recommendations[0].title)
        if not hints:
            hints.append("Продолжайте отслеживать метрики — так проще замечать прогресс.")

        return json.dumps({"hints": hints[:3]}, ensure_ascii=False)

    def _build_fallback_meal_plan(
        self,
        analytics: AnalyticsResponse,
        days: int = 7,
        user_context: str | None = None,
        *,
        is_vegetarian: bool | None = None,
    ) -> str:
        from app.llm.meal_plan_fallback import build_fallback_meal_plan_json

        return build_fallback_meal_plan_json(
            analytics,
            days=days,
            user_context=user_context,
            is_vegetarian=is_vegetarian,
        )

    def generate_meal_plan(
        self,
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        days: int = 7,
        *,
        is_vegetarian: bool = False,
        allergies_text: str | None = None,
    ) -> tuple[str, str]:
        prompt = PromptBuilder.build_meal_plan_prompt(
            user_context or "",
            days,
            is_vegetarian=is_vegetarian,
            allergies_text=allergies_text,
        )
        predict_budget = min(3800, 600 + days * 480)
        try:
            text = self.meal_plan_client.generate(
                prompt=prompt,
                system_prompt=PromptBuilder.JSON_SYSTEM_PROMPT,
                temperature=0.25,
                json_mode=True,
                ollama_options={
                    "num_predict": predict_budget,
                    "num_ctx": 4096,
                },
            )
            return text, "llm"
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return (
                    self._build_fallback_meal_plan(
                        analytics,
                        days=days,
                        user_context=user_context,
                        is_vegetarian=is_vegetarian,
                    ),
                    "fallback",
                )
            raise

    def generate_workout_plan(self, analytics: AnalyticsResponse, user_context: str | None = None, days: int = 7) -> str:
        prompt = PromptBuilder.build_workout_plan_prompt(analytics, user_context, days)
        return self.client.generate(
            prompt=prompt,
            system_prompt=PromptBuilder.JSON_SYSTEM_PROMPT,
            temperature=0.3,
            json_mode=True,
        )

    def generate_dashboard_hints(
        self,
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        dietary_rules: str | None = None,
    ) -> str:
        prompt = PromptBuilder.build_dashboard_hints_prompt(
            analytics,
            user_context,
            dietary_rules=dietary_rules,
        )
        try:
            return self.client.generate(
                prompt=prompt,
                system_prompt=PromptBuilder.JSON_SYSTEM_PROMPT,
                temperature=0.4,
                json_mode=True,
            )
        except LLMClientError:
            if settings.AI_FALLBACK_ENABLED:
                return self._build_fallback_dashboard_hints(analytics)
            raise

    def generate_proactive_tip(
        self,
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        dietary_rules: str | None = None,
    ) -> str:
        prompt = PromptBuilder.build_proactive_tip_prompt(
            analytics,
            user_context,
            dietary_rules=dietary_rules,
        )
        return sanitize_llm_markdown(
            self.client.generate(prompt=prompt, system_prompt=PromptBuilder.SYSTEM_PROMPT, temperature=0.4).strip()
        )