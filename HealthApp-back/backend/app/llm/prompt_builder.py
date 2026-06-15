from app.schemas.analytics import AnalyticsResponse


class PromptBuilder:
    SYSTEM_PROMPT = """
Ты — персональный AI-консультант по здоровью и образу жизни в приложении HealthApp.

Стиль: как внимательный чат-помощник — дружелюбно, по-русски, на «вы», короткими абзацами.

Правила:
1. Не ставь диагнозы и не назначай лечение, не отменяй лекарства.
2. Отвечай на ЛЮБОЙ вопрос о здоровье, сне, питании, воде, активности, стрессе, привычках.
3. Опирайся на блок «ДАННЫЕ ПОЛЬЗОВАТЕЛЯ» — цифры, даты, тренды. Не выдумывай факты.
4. Если данных нет — скажите честно и подскажите, что записать в дневнике.
5. Давай конкретные шаги: что, когда, сколько (мл, минут, порций), привязка к целям из профиля.
6. Учитывай сегодняшние показатели и отставание от целей.
7. При общих вопросах (например «как лучше спать») сочетай общие советы с персонализацией по данным.
8. Не повторяй дословно предыдущие ответы в диалоге, развивай тему.
9. Без markdown-заголовков и таблиц — обычный текст, списки через «•» или нумерацию при необходимости.
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
    def build_chat_messages(
        user_health_context: str,
        user_question: str,
        history: list[dict[str, str]] | None = None,
        dietary_rules: str | None = None,
    ) -> list[dict[str, str]]:
        """Сообщения для Ollama /api/chat."""
        system = (
            f"{PromptBuilder.SYSTEM_PROMPT}\n\n"
            f"--- ДАННЫЕ ПОЛЬЗОВАТЕЛЯ (дневник HealthApp) ---\n"
            f"{user_health_context.strip()}\n"
            f"--- КОНЕЦ ДАННЫХ ---"
        )
        if dietary_rules and dietary_rules.strip():
            system += f"\n\n{dietary_rules.strip()}"
        messages: list[dict[str, str]] = [{"role": "system", "content": system}]
        for item in history or []:
            role = item.get("role", "user")
            if role not in ("user", "assistant"):
                continue
            content = (item.get("content") or "").strip()
            if content:
                messages.append({"role": role, "content": content})
        messages.append({"role": "user", "content": user_question.strip()})
        return messages

    @staticmethod
    def build_chat_prompt(
        analytics: AnalyticsResponse,
        user_question: str,
        today: dict | None = None,
        personal_hints: list[dict] | None = None,
    ) -> str:
        """Legacy single-prompt (briefs)."""
        context = PromptBuilder.build_context_block(analytics)
        today_block = PromptBuilder.build_today_block(today)
        hints_block = PromptBuilder.build_personal_hints_block(personal_hints)
        extra = "\n\n".join(x for x in (today_block, hints_block) if x)
        return f"{context}\n{extra}\n\nВопрос: {user_question}".strip()

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
    def build_meal_plan_prompt(
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        days: int = 7,
    ) -> str:
        ctx = PromptBuilder.build_context_block(analytics)
        if user_context:
            ctx += f"\nДополнительный контекст:\n{user_context}"
        return f"""
ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}

ЗАДАЧА:
Сгенерируй персонализированный план питания на {days} дней.
ОБЯЗАТЕЛЬНО:
- Каждый день должен отличаться по блюдам (не повторяй одни и те же завтраки/обеды/ужины).
- Если в профиле указаны аллергии или ограничения — НЕ включай эти продукты ни в одно блюдо и список покупок.
- Если отмечено вегетарианство — СТРОГО без мяса, птицы, рыбы, морепродуктов, бульонов на мясе/рыбе.
- Примеры запрещённых ингредиентов при вегетарианстве: курица, говядина, свинина, индейка, рыба, креветки, тунец, лосось.
- Ориентируйся на целевые калории и КБЖУ из профиля, если они есть.
- Учитывай текущие показатели питания и рекомендации из контекста.

В ответ верни ТОЛЬКО валидный JSON без маркдауна (без ```json), который строго соответствует схеме:
{{
    "days": [
        {{
            "day_name": "Понедельник",
            "meals": [
                {{
                    "meal_type": "Завтрак",
                    "name": "Яичница с овощами",
                    "calories": 350,
                    "protein_g": 20.0,
                    "fat_g": 15.0,
                    "carbs_g": 25.0,
                    "recipe": "Краткий рецепт"
                }}
            ],
            "total_calories": 1500,
            "total_protein": 100.0,
            "total_fat": 50.0,
            "total_carbs": 150.0
        }}
    ],
    "grocery_list": [
        {{
            "category": "Овощи",
            "name": "Помидоры",
            "amount": "500 г"
        }}
    ]
}}
"""

    @staticmethod
    def build_workout_plan_prompt(
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        days: int = 7,
    ) -> str:
        ctx = PromptBuilder.build_context_block(analytics)
        if user_context:
            ctx += f"\nДополнительный контекст:\n{user_context}"
        return f"""
ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}

ЗАДАЧА:
Сгенерируй персонализированный план тренировок на {days} дней, учитывая уровень активности пользователя.
В ответ верни ТОЛЬКО валидный JSON без маркдауна (без ```json), который строго соответствует схеме:
{{
    "workouts": [
        {{
            "day_name": "Понедельник",
            "workout_type": "Силовая",
            "title": "Тренировка на все тело",
            "duration_minutes": 45,
            "description": "Описание упражнений..."
        }}
    ]
}}
"""
    @staticmethod
    def build_dashboard_hints_prompt(
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        dietary_rules: str | None = None,
    ) -> str:
        ctx = PromptBuilder.build_context_block(analytics)
        if user_context:
            ctx += f"\nДополнительный контекст:\n{user_context}"
        if dietary_rules and dietary_rules.strip():
            ctx += f"\n\n{dietary_rules.strip()}"
        return f"""
ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}

ЗАДАЧА:
Сгенерируй 2-3 короткие, максимально контекстные и полезные подсказки (hints) для дашборда на основе текущих данных (сон, вода, активность, питание).
Каждая подсказка должна быть 1 коротким предложением.
Если пользователь вегетарианец — не предлагайте мясо, птицу, рыбу и морепродукты в советах по питанию.
Примеры: "Вы спали всего 5 часов, постарайтесь лечь пораньше", "Вы отстаете по воде на 1л, выпейте стакан", "Сегодня вы сделали уже 8000 шагов, отличный результат!".
В ответ верни ТОЛЬКО валидный JSON без маркдауна (без ```json), который строго соответствует схеме:
{{
    "hints": ["Подсказка 1", "Подсказка 2", "Подсказка 3"]
}}
"""

    @staticmethod
    def build_proactive_tip_prompt(
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        dietary_rules: str | None = None,
    ) -> str:
        ctx = PromptBuilder.build_context_block(analytics)
        if user_context:
            ctx += f"\nДополнительный контекст:\n{user_context}"
        if dietary_rules and dietary_rules.strip():
            ctx += f"\n\n{dietary_rules.strip()}"
        return f"""
ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}

ЗАДАЧА:
Сгенерируй ОДИН короткий (1-2 предложения) персонализированный, проактивный совет (push-уведомление) для пользователя на основе текущего контекста и его метрик.
Совет должен быть заботливым и учитывать время суток и текущие данные по питанию, активности или гидратации.
Если пользователь вегетарианец — не предлагайте мясо, птицу, рыбу и морепродукты.
Например: "Время близится к вечеру, а вы выпили всего 1 литр воды — самое время для стакана воды!" или "Отличная прогулка, норма шагов почти выполнена!".
В ответ верни ТОЛЬКО текст совета, без кавычек и дополнительных пояснений.
"""

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

    @staticmethod
    def build_meal_plan_prompt(
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        days: int = 7,
    ) -> str:
        ctx = PromptBuilder.build_context_block(analytics)
        if user_context:
            ctx += f"\nДополнительный контекст:\n{user_context}"
        return f"""
ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}

ЗАДАЧА:
Сгенерируй персонализированный план питания на {days} дней.
ОБЯЗАТЕЛЬНО:
- Каждый день должен отличаться по блюдам (не повторяй одни и те же завтраки/обеды/ужины).
- Если в профиле указаны аллергии или ограничения — НЕ включай эти продукты ни в одно блюдо и список покупок.
- Если отмечено вегетарианство — СТРОГО без мяса, птицы, рыбы, морепродуктов, бульонов на мясе/рыбе.
- Примеры запрещённых ингредиентов при вегетарианстве: курица, говядина, свинина, индейка, рыба, креветки, тунец, лосось.
- Ориентируйся на целевые калории и КБЖУ из профиля, если они есть.
- Учитывай текущие показатели питания и рекомендации из контекста.

В ответ верни ТОЛЬКО валидный JSON без маркдауна (без ```json), который строго соответствует схеме:
{{
    "days": [
        {{
            "day_name": "Понедельник",
            "meals": [
                {{
                    "meal_type": "Завтрак",
                    "name": "Яичница с овощами",
                    "calories": 350,
                    "protein_g": 20.0,
                    "fat_g": 15.0,
                    "carbs_g": 25.0,
                    "recipe": "Краткий рецепт"
                }}
            ],
            "total_calories": 1500,
            "total_protein": 100.0,
            "total_fat": 50.0,
            "total_carbs": 150.0
        }}
    ],
    "grocery_list": [
        {{
            "category": "Овощи",
            "name": "Помидоры",
            "amount": "500 г"
        }}
    ]
}}
"""

    @staticmethod
    def build_workout_plan_prompt(
        analytics: AnalyticsResponse,
        user_context: str | None = None,
        days: int = 7,
    ) -> str:
        ctx = PromptBuilder.build_context_block(analytics)
        if user_context:
            ctx += f"\nДополнительный контекст:\n{user_context}"
        return f"""
ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}

ЗАДАЧА:
Сгенерируй персонализированный план тренировок на {days} дней, учитывая уровень активности пользователя.
В ответ верни ТОЛЬКО валидный JSON без маркдауна (без ```json), который строго соответствует схеме:
{{
    "workouts": [
        {{
            "day_name": "Понедельник",
            "workout_type": "Силовая",
            "title": "Тренировка на все тело",
            "duration_minutes": 45,
            "description": "Описание упражнений..."
        }}
    ]
}}
"""