from app.schemas.analytics import AnalyticsResponse


class PromptBuilder:
    SYSTEM_PROMPT = """
Ты — персональный AI-ассистент по здоровью в приложении HealthApp.

Твои правила:
1. Не ставь диагнозы.
2. Не говори, что у пользователя есть болезнь.
3. Не отменяй лекарства и не советуй лечение.
4. Опирайся только на переданные данные аналитики.
5. Если данных мало, честно скажи об этом.
6. Давай практичные, спокойные и понятные рекомендации.
7. Объясняй причинно-следственные связи человеческим языком.
8. Пиши на русском языке.
9. Не выдумывай факты, которых нет в данных.
10. Если видишь позитивный паттерн, тоже отмечай его.
""".strip()

    @staticmethod
    def build_context_block(analytics: AnalyticsResponse) -> str:
        insights_text = "\n".join(
            [
                f"- {item.title}: {item.description} "
                f"(confidence={item.confidence}, impact={item.impact}, severity={item.severity})"
                for item in analytics.insights
            ]
        ) or "- Нет инсайтов"

        recommendations_text = "\n".join(
            [
                f"- {item.title}: {item.description}. Действие: {item.action or 'не указано'} "
                f"(priority={item.priority}, confidence={item.confidence})"
                for item in analytics.recommendations
            ]
        ) or "- Нет рекомендаций"

        meta_message = analytics.meta.message or "Данных достаточно для базового анализа."

        return f"""
Период анализа: {analytics.summary.period_days} дней
Начало периода: {analytics.meta.start_date}
Конец периода: {analytics.meta.end_date}
Количество точек данных: {analytics.meta.data_points}
Достаточно данных: {analytics.meta.has_enough_data}
Комментарий по данным: {meta_message}

Scores:
- Health score: {analytics.summary.health_score}/100
- Sleep score: {analytics.summary.sleep_score}/100
- Hydration score: {analytics.summary.hydration_score}/100
- Activity score: {analytics.summary.activity_score}/100
- Nutrition score: {analytics.summary.nutrition_score}/100
- State score: {analytics.summary.state_score}/100

Insights:
{insights_text}

Recommendations:
{recommendations_text}
""".strip()

    @staticmethod
    def build_today_block(today: dict | None) -> str:
        if not today:
            return ""
        return f"""
Сегодня (факт / цель):
- Сон: {today.get('sleep_hours', 0):.1f} ч из {today.get('sleep_target', 8):.1f} ч
- Вода: {today.get('water_ml', 0)} мл из {today.get('water_target', 2500)} мл
- Шаги: {today.get('steps', 0)} из {today.get('steps_target', 10000)}
- Калории (еда): {today.get('calories', 0)} из {today.get('calories_target', 2200)} ккал
- Сожжено: {today.get('burned', 0)} из {today.get('burn_target', 500)} ккал
- Записей настроения сегодня: {'да' if today.get('state_logged') else 'нет'}
""".strip()

    @staticmethod
    def build_personal_hints_block(hints: list[dict] | None) -> str:
        if not hints:
            return ""
        lines = [
            f"- {h.get('title', '')}: {h.get('description', '')}"
            + (f" Действие: {h['action']}" if h.get("action") else "")
            for h in hints[:4]
        ]
        return "Персональные наблюдения из дневника:\n" + "\n".join(lines)

    @staticmethod
    def build_chat_prompt(
        analytics: AnalyticsResponse,
        user_question: str,
        today: dict | None = None,
        personal_hints: list[dict] | None = None,
    ) -> str:
        context = PromptBuilder.build_context_block(analytics)
        today_block = PromptBuilder.build_today_block(today)
        hints_block = PromptBuilder.build_personal_hints_block(personal_hints)

        extra = "\n\n".join(x for x in (today_block, hints_block) if x)

        return f"""
Ниже аналитический контекст пользователя.

{context}
{extra}

Вопрос пользователя:
{user_question}

Ответь:
1. Понятно и по-русски, на «вы» или «ты» как в вопросе
2. Без медицинских диагнозов и назначения лечения
3. Сначала ответь на вопрос; опирайся на блок «Сегодня» для советов на текущий день
4. Если данных мало — скажи честно, что записать (сон, вода, еда, шаги)
5. 2–4 коротких абзаца, без воды
6. В конце — нумерованный список из 1–3 конкретных шагов на сегодня или завтра
""".strip()

    @staticmethod
    def build_daily_brief_prompt(analytics: AnalyticsResponse) -> str:
        context = PromptBuilder.build_context_block(analytics)

        return f"""
Ниже аналитический контекст пользователя.

{context}

Сделай краткий daily brief:
1. Короткий заголовок
2. Краткое summary на 3-5 предложений
3. 3 ключевых пункта в виде коротких фраз
4. Без диагнозов
5. С фокусом на практические действия сегодня
""".strip()

    @staticmethod
    def build_weekly_brief_prompt(analytics: AnalyticsResponse) -> str:
        context = PromptBuilder.build_context_block(analytics)

        return f"""
Ниже аналитический контекст пользователя.

{context}

Сделай weekly brief:
1. Общая оценка недели
2. Что было слабым местом
3. Что было сильным местом
4. Какие 2-3 привычки улучшить на следующей неделе
5. Без диагнозов
""".strip()

    @staticmethod
    def build_explain_insight_prompt(
        analytics: AnalyticsResponse,
        insight_title: str,
    ) -> str:
        context = PromptBuilder.build_context_block(analytics)

        return f"""
Ниже аналитический контекст пользователя.

{context}

Нужно объяснить пользователю инсайт:
"{insight_title}"

Ответь:
1. Что означает этот инсайт
2. Почему система могла к нему прийти
3. Что это значит на практике
4. Что пользователь может попробовать сделать
5. Без диагнозов, спокойно и понятно
""".strip()