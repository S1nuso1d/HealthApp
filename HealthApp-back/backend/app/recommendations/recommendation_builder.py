from app.models.insight import Insight
from app.schemas.analytics import RecommendationItem


class RecommendationBuilder:
    @staticmethod
    def build_from_insight(insight: Insight, priority: str) -> RecommendationItem | None:
        """
        Преобразует Insight -> RecommendationItem.
        Возвращает None, если для инсайта пока нет шаблона рекомендации.
        """

        if insight.insight_type == "low_sleep_duration":
            return RecommendationItem(
                category="sleep",
                title="Увеличь продолжительность сна",
                description=(
                    "Средняя длительность сна ниже желаемого уровня. "
                    "Попробуй стабилизировать вечерний режим и увеличить общее время сна."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Попробуй ложиться на 30–60 минут раньше в течение 7 дней.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "low_hydration":
            return RecommendationItem(
                category="hydration",
                title="Увеличь потребление воды",
                description=(
                    "У тебя заметен недобор жидкости. Более равномерная гидратация в течение дня "
                    "может улучшить самочувствие и восстановление."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Добавь 2–3 дополнительных приема воды по 250 мл в день.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "high_caffeine_intake":
            return RecommendationItem(
                category="meals",
                title="Снизь потребление кофеина",
                description=(
                    "Высокий уровень кофеина может мешать сну и восстановлению, "
                    "особенно во второй половине дня."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Попробуй ограничить кофеин после 15:00 в течение недели.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "low_daily_activity":
            return RecommendationItem(
                category="activity",
                title="Повышай ежедневную активность",
                description=(
                    "Низкая повседневная активность может ухудшать общее состояние, "
                    "энергию и качество восстановления."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Добавь ежедневную прогулку на 20–30 минут.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "low_subjective_state":
            return RecommendationItem(
                category="state",
                title="Снизь нагрузку и сфокусируйся на восстановлении",
                description=(
                    "Субъективное состояние снижено. Это может говорить об усталости, "
                    "стрессе или перегрузке."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="На ближайшие 2–3 дня сделай сон и восстановление главным приоритетом.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        # ---------------- CORRELATION INSIGHTS ----------------

        if insight.insight_type == "late_caffeine_sleep_impact":
            return RecommendationItem(
                category="correlation",
                title="Смести кофеин на более раннее время",
                description=(
                    "Похоже, поздний кофеин связан с ухудшением сна. "
                    "Лучше сократить напитки с кофеином во второй половине дня."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Попробуй 7 дней не употреблять кофеин после 15:00.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "late_meal_sleep_impact":
            return RecommendationItem(
                category="correlation",
                title="Заверши прием пищи раньше вечером",
                description=(
                    "Поздние приемы пищи могут быть связаны с более поздним засыпанием "
                    "или ухудшением качества сна."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Попробуй заканчивать ужин за 2–3 часа до сна.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "low_hydration_low_energy":
            return RecommendationItem(
                category="correlation",
                title="Повышай гидратацию в дни с нагрузкой",
                description=(
                    "Похоже, в дни с недостатком жидкости уровень энергии ниже. "
                    "Более стабильная гидратация может помочь самочувствию."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Добавь воду в первую половину дня и после активности.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "short_sleep_low_energy":
            return RecommendationItem(
                category="correlation",
                title="Сделай сон главным источником восстановления",
                description=(
                    "Короткий сон может быть напрямую связан со снижением энергии на следующий день."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Поставь цель 2–3 дня подряд спать не меньше 7 часов.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "evening_high_activity_sleep_impact":
            return RecommendationItem(
                category="correlation",
                title="Снизь интенсивность вечерних тренировок",
                description=(
                    "Поздняя интенсивная активность может мешать расслаблению и ухудшать сон."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Перенеси интенсивные тренировки раньше или замени их вечером на легкую активность.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        if insight.insight_type == "hydration_activity_energy_positive":
            return RecommendationItem(
                category="correlation",
                title="Сохраняй связку вода + активность",
                description=(
                    "У тебя есть позитивный паттерн: дни с хорошей гидратацией и активностью "
                    "связаны с более высокой энергией."
                ),
                priority=priority,
                confidence=insight.confidence,
                action="Старайся поддерживать этот режим как базовый шаблон дня.",
                related_insight_title=insight.title,
                related_insight_type=insight.insight_type,
            )

        return None