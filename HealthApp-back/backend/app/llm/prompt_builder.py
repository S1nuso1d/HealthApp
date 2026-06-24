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
9. Оформление ответа для мобильного чата:
   - НИКОГДА не используйте markdown: запрещены #, ##, ###, **, __, ``` и любые звёздочки для выделения.
   - Заголовки разделов — отдельной строкой обычным текстом (например: «Сон» или «Что улучшить сегодня»).
   - Списки — через «•» или нумерацию «1.», «2.» с новой строки.
   - Между абзацами — пустая строка; ответ читается как аккуратная заметка, а не как сырой markdown.
10. Учитывайте блок «ТЕКУЩЕЕ ВРЕМЯ» в данных пользователя: вечером и ночью предлагайте готовиться ко сну, утром — воду и завтрак, днём — активность и обед.
11. Отвечайте ТОЛЬКО на русском языке. Никогда не используйте китайский, английский или другие языки.
""".strip()

    JSON_SYSTEM_PROMPT = """
Ты — персональный AI-консультант по здоровью в приложении HealthApp.

Правила:
1. Отвечай ТОЛЬКО валидным JSON-объектом — без markdown, без ```json, без текста до или после JSON.
2. Строго соблюдай схему из запроса пользователя.
3. Не ставь диагнозы и не назначай лечение.
4. Все строковые значения — на русском языке.
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
        user_context: str,
        days: int = 7,
        *,
        is_vegetarian: bool = False,
        allergies_text: str | None = None,
    ) -> str:
        ctx = user_context.strip()
        diet_rules = []
        if is_vegetarian:
            diet_rules.append("- Вегетарианское меню: без мяса, птицы, рыбы и морепродуктов.")
        else:
            diet_rules.append(
                "- Обычное меню: включай курицу, индейку, говядину или рыбу "
                "минимум в одном приёме (обед или ужин) каждый день, если нет аллергии."
            )
        if allergies_text and allergies_text.strip():
            diet_rules.append(f"- Строго исключи: {allergies_text.strip()}")
        diet_block = "\n".join(diet_rules)
        return f"""
ЗАДАЧА:
Сгенерируй план питания ровно на {days} дней. Простые домашние блюда, без ресторанной экзотики.
Примеры: каша, гречка с курицей, борщ, суп, макароны, котлеты, рис с рыбой, омлет, творог, салат, жареная картошка.
{diet_block}
ОБЯЗАТЕЛЬНО:
- В каждом дне ровно 3 приёма: Завтрак, Обед, Ужин.
- Каждый день — разные блюда.
- В каждом блюде укажи ingredients — список компонентов с граммовкой.
  Для составных блюд (например «Гречка с курицей») — отдельно гречка и курица.
- recipe — до 25 символов или пустая строка "".
- Ориентируйся на «Цель КБЖУ в день» (допуск ±10%).
- НЕ генерируй grocery_list — только days.

В ответ верни ТОЛЬКО валидный JSON без маркдауна:
{{
    "days": [
        {{
            "day_name": "Понедельник",
            "meals": [
                {{
                    "meal_type": "Завтрак",
                    "name": "Овсянка с ягодами",
                    "calories": 350,
                    "protein_g": 12.0,
                    "fat_g": 8.0,
                    "carbs_g": 55.0,
                    "recipe": "",
                    "ingredients": [
                        {{"name": "Овсянка", "grams_g": 50}},
                        {{"name": "Молоко", "grams_g": 200}}
                    ]
                }},
                {{
                    "meal_type": "Обед",
                    "name": "Гречка с курицей",
                    "calories": 520,
                    "protein_g": 38.0,
                    "fat_g": 12.0,
                    "carbs_g": 58.0,
                    "recipe": "",
                    "ingredients": [
                        {{"name": "Гречка", "grams_g": 150}},
                        {{"name": "Курица", "grams_g": 120}}
                    ]
                }},
                {{
                    "meal_type": "Ужин",
                    "name": "Творог со сметаной",
                    "calories": 320,
                    "protein_g": 28.0,
                    "fat_g": 10.0,
                    "carbs_g": 18.0,
                    "recipe": "",
                    "ingredients": [
                        {{"name": "Творог", "grams_g": 150}},
                        {{"name": "Сметана", "grams_g": 30}}
                    ]
                }}
            ],
            "total_calories": 1190,
            "total_protein": 78.0,
            "total_fat": 30.0,
            "total_carbs": 131.0
        }}
    ]
}}

ДАННЫЕ:
{ctx}
""".strip()

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

ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}
""".strip()

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
ЗАДАЧА:
Сгенерируй 2-3 короткие, максимально контекстные и полезные подсказки (hints) для дашборда на основе текущих данных (сон, вода, активность, питание).
Каждая подсказка — одно короткое предложение, не длиннее 65 символов (с пробелами). Без лишних слов и без маркдауна.
ОБЯЗАТЕЛЬНО учитывайте блок «ТЕКУЩЕЕ ВРЕМЯ»: вечером (после 21:00) и ночью предлагайте лечь спать; утром — воду и завтрак; днём — шаги и обед.
Если пользователь вегетарианец — не предлагайте мясо, птицу, рыбу и морепродукты в советах по питанию.
Примеры: "Сон 5 ч — лягте пораньше", "Не хватает воды — выпейте стакан", "8000 шагов — отличный результат".
В ответ верни ТОЛЬКО валидный JSON без маркдауна (без ```json), который строго соответствует схеме:
{{
    "hints": ["Подсказка 1", "Подсказка 2", "Подсказка 3"]
}}

ДАННЫЕ ПОЛЬЗОВАТЕЛЯ:
{ctx}
""".strip()

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
Совет должен быть заботливым и ОБЯЗАТЕЛЬНО учитывать блок «ТЕКУЩЕЕ ВРЕМЯ» в данных: вечером и ночью — мягко предложите лечь спать; утром — воду/завтрак; днём — активность или обед.
Если пользователь вегетарианец — не предлагайте мясо, птицу, рыбу и морепродукты.
Например: "Уже 22:30 — пора завершать дела и лечь, чтобы выспаться" или "Доброе утро! Начните день со стакана воды."
В ответ верни ТОЛЬКО текст совета, без кавычек, без markdown и без дополнительных пояснений.
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