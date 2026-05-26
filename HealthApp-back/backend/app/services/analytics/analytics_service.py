from __future__ import annotations

from datetime import datetime, timedelta, timezone
from statistics import mean
from typing import Any

from sqlalchemy.orm import Session

from app.models.action_plan import ActionPlan
from app.models.activity import ActivityRecord
from app.models.analysis_run import AnalysisRun
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.saved_recommendation import SavedRecommendation
from app.models.sleep import SleepRecord
from app.models.user_state import UserState
from app.services.analytics.action_plan_service import build_action_plan_from_recommendations


def safe_mean(values: list[float | int]) -> float:
    filtered = [float(v) for v in values if v is not None]
    if not filtered:
        return 0.0
    return mean(filtered)


def clamp_score(value: float) -> int:
    value = round(value)
    if value < 0:
        return 0
    if value > 100:
        return 100
    return int(value)


def avg_or_zero(items: list[Any], attr: str) -> float:
    values = []
    for item in items:
        value = getattr(item, attr, None)
        if value is not None:
            values.append(float(value))
    return safe_mean(values)


def build_insight(
    category: str,
    title: str,
    description: str,
    confidence: float,
    impact: str,
    why_this: str | None = None,
    based_on: str | None = None,
    expected_effect: str | None = None,
) -> dict:
    return {
        "category": category,
        "title": title,
        "description": description,
        "confidence": round(confidence, 2),
        "impact": impact,
        "why_this": why_this,
        "based_on": based_on,
        "expected_effect": expected_effect,
    }


def build_recommendation(
    category: str,
    title: str,
    description: str,
    priority: str,
    confidence: float,
    action: str | None = None,
    why_this: str | None = None,
    based_on: str | None = None,
    expected_effect: str | None = None,
) -> dict:
    return {
        "category": category,
        "title": title,
        "description": description,
        "priority": priority,
        "confidence": round(confidence, 2),
        "action": action,
        "why_this": why_this,
        "based_on": based_on,
        "expected_effect": expected_effect,
    }


def deduplicate_recommendations(items: list[dict]) -> list[dict]:
    seen = set()
    result = []
    for item in items:
        key = (item["category"], item["title"])
        if key not in seen:
            seen.add(key)
            result.append(item)
    return result


def deduplicate_insights(items: list[dict]) -> list[dict]:
    seen = set()
    result = []
    for item in items:
        key = (item["category"], item["title"])
        if key not in seen:
            seen.add(key)
            result.append(item)
    return result


def sort_recommendations(items: list[dict]) -> list[dict]:
    priority_order = {"high": 0, "medium": 1, "low": 2}
    return sorted(
        items,
        key=lambda x: (priority_order.get(x["priority"], 3), -x.get("confidence", 0.0))
    )


def calculate_health_score(
    sleep_score: int,
    hydration_score: int,
    activity_score: int,
    nutrition_score: int,
    state_score: int,
) -> int:
    raw = (
        sleep_score * 0.30 +
        hydration_score * 0.15 +
        activity_score * 0.20 +
        nutrition_score * 0.20 +
        state_score * 0.15
    )
    return clamp_score(raw)


def analyze_sleep(sleeps: list[SleepRecord]) -> tuple[list[dict], list[dict], int]:
    insights = []
    recommendations = []

    if not sleeps:
        recommendations.append(build_recommendation(
            category="sleep",
            title="Недостаточно данных о сне",
            description="У вас пока мало данных о сне. Для точного анализа записывайте сон хотя бы 5–7 дней подряд.",
            priority="medium",
            confidence=0.95,
            action="Добавляйте записи сна ежедневно в течение недели.",
            why_this="Система не нашла достаточного количества записей сна за выбранный период.",
            based_on="Меньше минимального объема данных для надежного анализа сна.",
            expected_effect="После накопления данных рекомендации по сну станут точнее."
        ))
        return insights, recommendations, 50

    avg_sleep = avg_or_zero(sleeps, "duration_hours")
    avg_latency = avg_or_zero(sleeps, "sleep_latency_minutes")
    avg_efficiency = avg_or_zero(sleeps, "sleep_efficiency")
    avg_awakenings = avg_or_zero(sleeps, "awakenings_count")

    durations = [float(s.duration_hours) for s in sleeps if s.duration_hours is not None]
    sleep_variability = 0.0
    if len(durations) >= 2:
        sleep_variability = max(durations) - min(durations)

    sleep_score = 100.0

    if avg_sleep < 6:
        sleep_score -= 25
        insights.append(build_insight(
            category="sleep",
            title="Недостаток сна",
            description=f"Средняя длительность сна за период составляет {avg_sleep:.1f} ч.",
            confidence=0.95,
            impact="negative",
            why_this="Средний сон ниже базовой здоровой зоны.",
            based_on=f"Средняя длительность сна: {avg_sleep:.1f} ч.",
            expected_effect="Увеличение сна обычно улучшает энергию, концентрацию и восстановление."
        ))
        recommendations.append(build_recommendation(
            category="sleep",
            title="Увеличить длительность сна",
            description=f"Сейчас вы спите в среднем {avg_sleep:.1f} часа. Это может ухудшать восстановление, энергию и концентрацию.",
            priority="high",
            confidence=0.95,
            action="Постарайтесь увеличить сон хотя бы до 7–8 часов в течение ближайших 7 дней.",
            why_this="Зафиксирован устойчивый недосып по среднему показателю сна.",
            based_on=f"Средняя длительность сна за период: {avg_sleep:.1f} ч.",
            expected_effect="Даже +40–60 минут сна могут заметно повысить энергию и качество самочувствия."
        ))
    elif avg_sleep < 7:
        sleep_score -= 12
        recommendations.append(build_recommendation(
            category="sleep",
            title="Сон немного ниже оптимума",
            description=f"Средняя длительность сна составляет {avg_sleep:.1f} часа.",
            priority="medium",
            confidence=0.8,
            action="Сместите время отхода ко сну на 20–40 минут раньше.",
            why_this="Сон не критически низкий, но ниже оптимального диапазона.",
            based_on=f"Средняя длительность сна: {avg_sleep:.1f} ч.",
            expected_effect="Небольшое увеличение сна может улучшить утреннюю бодрость."
        ))
    else:
        insights.append(build_insight(
            category="sleep",
            title="Достаточная длительность сна",
            description=f"Средняя длительность сна составляет {avg_sleep:.1f} часа.",
            confidence=0.8,
            impact="positive",
            why_this="Средняя длительность сна находится в приемлемой зоне.",
            based_on=f"Средняя длительность сна: {avg_sleep:.1f} ч.",
            expected_effect="Поддержание этого уровня сна помогает стабилизировать состояние."
        ))

    if avg_latency > 30:
        sleep_score -= 15
        insights.append(build_insight(
            category="sleep",
            title="Долгое засыпание",
            description=f"В среднем засыпание занимает около {avg_latency:.0f} минут.",
            confidence=0.85,
            impact="negative",
            why_this="Среднее время засыпания превышает нормальную комфортную зону.",
            based_on=f"Средняя латентность сна: {avg_latency:.0f} мин.",
            expected_effect="Снижение времени засыпания улучшает субъективное качество сна."
        ))
        recommendations.append(build_recommendation(
            category="sleep",
            title="Сократить время засыпания",
            description="Долгое засыпание может быть связано со стрессом, поздней едой, кофеином или вечерней нагрузкой.",
            priority="high",
            confidence=0.85,
            action="Проверьте, нет ли кофеина, плотной еды или интенсивной активности за 2–4 часа до сна.",
            why_this="Система обнаружила устойчиво высокую латентность сна.",
            based_on=f"Средняя латентность сна: {avg_latency:.0f} мин.",
            expected_effect="Устранение вечерних триггеров должно сократить время засыпания."
        ))

    if avg_efficiency > 0:
        if avg_efficiency < 85:
            sleep_score -= 12
            insights.append(build_insight(
                category="sleep",
                title="Низкая эффективность сна",
                description=f"Средняя эффективность сна составляет {avg_efficiency:.0f}%.",
                confidence=0.8,
                impact="negative",
                why_this="Эффективность сна ниже комфортного уровня.",
                based_on=f"Средняя эффективность сна: {avg_efficiency:.0f}%.",
                expected_effect="Стабильный режим и уменьшение ночных помех могут улучшить непрерывность сна."
            ))
            recommendations.append(build_recommendation(
                category="sleep",
                title="Улучшить непрерывность сна",
                description="Сон может быть поверхностным или прерывистым.",
                priority="medium",
                confidence=0.8,
                action="Старайтесь не пить много жидкости поздно вечером и ложиться примерно в одно и то же время.",
                why_this="Сон выглядит фрагментированным по эффективности.",
                based_on=f"Средняя эффективность сна: {avg_efficiency:.0f}%.",
                expected_effect="Повышение эффективности сна обычно уменьшает усталость утром."
            ))

    if sleep_variability > 2:
        sleep_score -= 10
        recommendations.append(build_recommendation(
            category="sleep",
            title="Стабилизировать режим сна",
            description="Нестабильный сон ухудшает восстановление даже при нормальной средней длительности.",
            priority="medium",
            confidence=0.75,
            action="Старайтесь держать разницу между ночами не больше 1 часа.",
            why_this="Разброс длительности сна слишком высокий.",
            based_on=f"Разброс сна: {sleep_variability:.1f} ч.",
            expected_effect="Стабильный режим обычно делает сон глубже и предсказуемее."
        ))

    if avg_awakenings >= 3:
        sleep_score -= 8
        recommendations.append(build_recommendation(
            category="sleep",
            title="Снизить ночные пробуждения",
            description=f"У вас в среднем около {avg_awakenings:.1f} пробуждений за ночь.",
            priority="medium",
            confidence=0.7,
            action="Проверьте поздние напитки, стресс и вечерние стимуляторы.",
            why_this="Количество ночных пробуждений выше желаемого.",
            based_on=f"Среднее число пробуждений: {avg_awakenings:.1f}.",
            expected_effect="Сокращение пробуждений повысит качество восстановления."
        ))

    return insights, recommendations, clamp_score(sleep_score)


def analyze_meals(meals: list[MealRecord]) -> tuple[list[dict], list[dict], int]:
    insights = []
    recommendations = []

    if not meals:
        recommendations.append(build_recommendation(
            category="meals",
            title="Недостаточно данных о питании",
            description="Пока мало данных о питании, поэтому анализ пищевых факторов ограничен.",
            priority="medium",
            confidence=0.95,
            action="Добавляйте основные приемы пищи и напитки хотя бы 5–7 дней.",
            why_this="Недостаточный объем пищевых записей.",
            based_on="Слишком мало данных о питании за выбранный период.",
            expected_effect="После накопления записей рекомендации по питанию станут персональнее."
        ))
        return insights, recommendations, 50

    nutrition_score = 100.0

    late_meals = [m for m in meals if m.is_late_meal == 1]
    late_meal_ratio = len(late_meals) / len(meals) if meals else 0

    evening_caffeine = [
        m for m in meals
        if (m.caffeine_mg or 0) >= 50 and (
            (m.minutes_before_sleep is not None and m.minutes_before_sleep < 360)
            or (m.meal_time.hour >= 16)
        )
    ]

    avg_protein_per_meal = avg_or_zero(meals, "protein_g")

    if late_meal_ratio >= 0.25:
        nutrition_score -= 18
        recommendations.append(build_recommendation(
            category="meals",
            title="Сдвинуть поздние приемы пищи раньше",
            description="Поздняя еда может ухудшать засыпание, качество сна и общее восстановление.",
            priority="high",
            confidence=0.9,
            action="Старайтесь завершать плотный прием пищи минимум за 2–3 часа до сна.",
            why_this="Доля поздних приемов пищи заметно выше комфортного уровня.",
            based_on=f"Поздние приемы пищи: {late_meal_ratio * 100:.0f}% записей.",
            expected_effect="Более ранний ужин может улучшить засыпание и качество сна."
        ))

    if len(evening_caffeine) >= 2:
        nutrition_score -= 15
        recommendations.append(build_recommendation(
            category="meals",
            title="Ограничить кофеин во второй половине дня",
            description="Поздний кофеин часто мешает засыпанию и снижает глубину сна.",
            priority="high",
            confidence=0.92,
            action="Попробуйте не употреблять кофеин после 15:00–16:00 в течение недели.",
            why_this="Обнаружены повторяющиеся случаи вечернего кофеина.",
            based_on=f"Количество случаев позднего кофеина: {len(evening_caffeine)}.",
            expected_effect="Снижение кофеина во второй половине дня часто уменьшает время засыпания."
        ))

    if avg_protein_per_meal < 15:
        nutrition_score -= 8
        recommendations.append(build_recommendation(
            category="meals",
            title="Добавить белок в рацион",
            description=f"Среднее количество белка на один прием пищи около {avg_protein_per_meal:.1f} г.",
            priority="medium",
            confidence=0.68,
            action="Добавьте в основные приемы пищи источники белка: яйца, рыбу, мясо, творог, бобовые.",
            why_this="Средний белок на прием пищи ниже желаемого.",
            based_on=f"Средний белок на прием пищи: {avg_protein_per_meal:.1f} г.",
            expected_effect="Повышение белка может улучшить сытость, восстановление и стабильность энергии."
        ))

    return insights, recommendations, clamp_score(nutrition_score)


def analyze_hydration(hydration: list[HydrationRecord]) -> tuple[list[dict], list[dict], int]:
    insights = []
    recommendations = []

    if not hydration:
        recommendations.append(build_recommendation(
            category="hydration",
            title="Недостаточно данных о воде",
            description="Пока мало данных о гидратации, поэтому рекомендации по воде ограничены.",
            priority="medium",
            confidence=0.95,
            action="Начните записывать воду и другие напитки в течение нескольких дней.",
            why_this="Недостаточно записей о жидкости.",
            based_on="Слишком мало hydration-записей за период.",
            expected_effect="После накопления данных система сможет точнее связать воду с энергией и активностью."
        ))
        return insights, recommendations, 50

    hydration_score = 100.0
    daily_effective = {}

    for item in hydration:
        day_key = item.record_time.date().isoformat()
        daily_effective.setdefault(day_key, 0.0)
        amount = float(item.amount_ml or 0)
        factor = float(item.hydration_factor) if item.hydration_factor is not None else 1.0
        daily_effective[day_key] += amount * factor

    avg_effective = safe_mean(list(daily_effective.values()))
    late_drinks = [h for h in hydration if h.is_late_drink == 1]

    if avg_effective < 1500:
        hydration_score -= 20
        recommendations.append(build_recommendation(
            category="hydration",
            title="Увеличить дневную гидратацию",
            description="Недостаток жидкости может влиять на энергию, концентрацию и работоспособность.",
            priority="high",
            confidence=0.9,
            action="Постарайтесь выйти хотя бы на 1800–2200 мл эффективной гидратации в день.",
            why_this="Средний эффективный объем жидкости ниже базового ориентира.",
            based_on=f"Средний эффективный объем: {avg_effective:.0f} мл/день.",
            expected_effect="Увеличение воды часто улучшает энергию, концентрацию и общее самочувствие."
        ))
    elif avg_effective < 2000:
        hydration_score -= 10
        recommendations.append(build_recommendation(
            category="hydration",
            title="Немного повысить потребление воды",
            description=f"Средний объем жидкости около {avg_effective:.0f} мл в день.",
            priority="medium",
            confidence=0.75,
            action="Добавьте 1–2 дополнительных стакана воды в первой половине дня.",
            why_this="Гидратация близка к норме, но есть пространство для улучшения.",
            based_on=f"Средний эффективный объем: {avg_effective:.0f} мл/день.",
            expected_effect="Небольшое повышение воды может улучшить дневную устойчивость энергии."
        ))

    if len(late_drinks) >= 2:
        hydration_score -= 5
        recommendations.append(build_recommendation(
            category="hydration",
            title="Снизить объем жидкости поздно вечером",
            description="Поздние напитки могут повышать вероятность ночных пробуждений.",
            priority="low",
            confidence=0.65,
            action="Старайтесь переносить основной объем воды на первую половину дня.",
            why_this="Обнаружены повторяющиеся поздние напитки.",
            based_on=f"Поздние напитки: {len(late_drinks)} случаев.",
            expected_effect="Меньше поздней жидкости — меньше шансов на ночные пробуждения."
        ))

    return insights, recommendations, clamp_score(hydration_score)


def analyze_activity(activities: list[ActivityRecord]) -> tuple[list[dict], list[dict], int]:
    insights = []
    recommendations = []

    if not activities:
        recommendations.append(build_recommendation(
            category="activity",
            title="Недостаточно данных об активности",
            description="Пока мало данных об активности, поэтому анализ движения ограничен.",
            priority="medium",
            confidence=0.95,
            action="Добавляйте прогулки, тренировки и другую активность хотя бы 5–7 дней.",
            why_this="Недостаточный объем activity-записей.",
            based_on="Недостаточно данных для устойчивых выводов по активности.",
            expected_effect="После накопления данных система сможет точнее оценить влияние активности на сон и энергию."
        ))
        return insights, recommendations, 50

    activity_score = 100.0

    total_duration = sum((a.duration_minutes or 0) for a in activities)
    evening_high = [
        a for a in activities
        if a.is_evening_activity == 1 and (a.intensity or "").lower() == "high"
    ]
    evening_walks = [
        a for a in activities
        if a.is_evening_activity == 1 and (a.activity_type or "").lower() == "walk"
    ]

    if total_duration < 90:
        activity_score -= 20
        recommendations.append(build_recommendation(
            category="activity",
            title="Повысить общий объем движения",
            description="Недостаток активности может ухудшать сон, энергию и общее самочувствие.",
            priority="high",
            confidence=0.85,
            action="Добавьте ежедневную прогулку 20–30 минут или 3–4 короткие сессии активности в неделю.",
            why_this="Суммарной активности за период недостаточно.",
            based_on=f"Суммарная активность за период: {total_duration} мин.",
            expected_effect="Регулярное движение обычно улучшает сон, энергию и настроение."
        ))

    if len(evening_high) >= 2:
        activity_score -= 15
        recommendations.append(build_recommendation(
            category="activity",
            title="Сместить интенсивные тренировки раньше",
            description="Интенсивная нагрузка поздно вечером может мешать расслаблению и засыпанию.",
            priority="high",
            confidence=0.88,
            action="Старайтесь завершать интенсивные тренировки минимум за 3 часа до сна.",
            why_this="Обнаружены повторяющиеся поздние интенсивные сессии.",
            based_on=f"Поздняя интенсивная активность: {len(evening_high)} случаев.",
            expected_effect="Более ранняя тренировка часто снижает риск долгого засыпания."
        ))

    if len(evening_walks) >= 2:
        recommendations.append(build_recommendation(
            category="activity",
            title="Сохранить вечерние прогулки",
            description="Неспешные прогулки вечером часто положительно влияют на сон и уровень стресса.",
            priority="low",
            confidence=0.8,
            action="Продолжайте 20–40 минут спокойной ходьбы в вечернее время.",
            why_this="Вечерние прогулки уже присутствуют как полезная привычка.",
            based_on=f"Вечерние прогулки: {len(evening_walks)} случаев.",
            expected_effect="Поддержание этой привычки помогает расслаблению и восстановлению."
        ))

    return insights, recommendations, clamp_score(activity_score)


def analyze_state(states: list[UserState]) -> tuple[list[dict], list[dict], int]:
    insights = []
    recommendations = []

    if not states:
        recommendations.append(build_recommendation(
            category="state",
            title="Недостаточно данных о самочувствии",
            description="Пока нет достаточных записей настроения, энергии и стресса.",
            priority="low",
            confidence=0.95,
            action="Добавляйте оценку энергии, настроения и стресса хотя бы раз в день.",
            why_this="Нет достаточного объема субъективных self-report данных.",
            based_on="Недостаточно записей состояния за период.",
            expected_effect="После накопления self-report данных система точнее свяжет поведение и самочувствие."
        ))
        return insights, recommendations, 50

    state_score = 100.0

    avg_energy = avg_or_zero(states, "energy")
    avg_stress = avg_or_zero(states, "stress")
    avg_focus = avg_or_zero(states, "focus")

    if avg_energy < 5:
        state_score -= 20
        recommendations.append(build_recommendation(
            category="state",
            title="Разобраться с источниками усталости",
            description="Низкая энергия часто связана с недосыпом, обезвоживанием, стрессом или перегрузкой.",
            priority="high",
            confidence=0.9,
            action="Проверьте сон, воду и уровень вечерней нагрузки.",
            why_this="Средний уровень энергии устойчиво ниже комфортного уровня.",
            based_on=f"Средняя энергия: {avg_energy:.1f} из 10.",
            expected_effect="Устранение базовых триггеров обычно повышает дневную бодрость."
        ))

    if avg_stress >= 7:
        state_score -= 15
        recommendations.append(build_recommendation(
            category="state",
            title="Снизить уровень стресса",
            description="Высокий стресс может ухудшать сон, концентрацию и восстановление.",
            priority="high",
            confidence=0.85,
            action="Добавьте спокойную вечернюю рутину: прогулку, дыхательные практики или уменьшение экранного времени.",
            why_this="Стресс стабильно находится на высоком уровне.",
            based_on=f"Средний стресс: {avg_stress:.1f} из 10.",
            expected_effect="Снижение вечернего напряжения часто улучшает сон и фокус."
        ))

    if avg_focus < 5:
        state_score -= 10
        recommendations.append(build_recommendation(
            category="state",
            title="Поработать над концентрацией",
            description=f"Средний уровень фокуса составляет около {avg_focus:.1f} из 10.",
            priority="medium",
            confidence=0.72,
            action="Попробуйте стабилизировать сон, воду и снизить вечерние стимуляторы.",
            why_this="Фокус снижен на протяжении периода.",
            based_on=f"Средний фокус: {avg_focus:.1f} из 10.",
            expected_effect="Стабильный сон и вода часто улучшают концентрацию."
        ))

    return insights, recommendations, clamp_score(state_score)


def aggregate_daily_data(
    sleeps: list[SleepRecord],
    meals: list[MealRecord],
    hydration: list[HydrationRecord],
    activities: list[ActivityRecord],
    states: list[UserState],
) -> dict[str, dict]:
    daily: dict[str, dict] = {}

    def ensure_day(day_key: str) -> dict:
        if day_key not in daily:
            daily[day_key] = {
                "sleep_duration_hours": None,
                "sleep_quality_score": None,
                "sleep_latency_minutes": None,
                "late_caffeine": 0,
                "late_meal": 0,
                "hydration_effective_ml": 0.0,
                "evening_high_activity": 0,
                "evening_walk": 0,
                "energy": None,
            }
        return daily[day_key]

    for s in sleeps:
        day_key = s.sleep_end.date().isoformat()
        day = ensure_day(day_key)
        day["sleep_duration_hours"] = s.duration_hours
        day["sleep_quality_score"] = s.quality_score
        day["sleep_latency_minutes"] = s.sleep_latency_minutes

    for m in meals:
        day_key = m.meal_time.date().isoformat()
        day = ensure_day(day_key)

        if (m.caffeine_mg or 0) >= 50 and (
            (m.minutes_before_sleep is not None and m.minutes_before_sleep < 360)
            or (m.meal_time.hour >= 16)
        ):
            day["late_caffeine"] = 1

        if m.is_late_meal == 1:
            day["late_meal"] = 1

    for h in hydration:
        day_key = h.record_time.date().isoformat()
        day = ensure_day(day_key)

        amount = float(h.amount_ml or 0)
        factor = float(h.hydration_factor) if h.hydration_factor is not None else 1.0
        day["hydration_effective_ml"] += amount * factor

    for a in activities:
        day_key = a.start_time.date().isoformat()
        day = ensure_day(day_key)

        if a.is_evening_activity == 1 and (a.intensity or "").lower() == "high":
            day["evening_high_activity"] = 1

        if a.is_evening_activity == 1 and (a.activity_type or "").lower() == "walk":
            day["evening_walk"] = 1

    for st in states:
        day_key = st.record_time.date().isoformat()
        day = ensure_day(day_key)
        day["energy"] = st.energy

    return daily


def compare_groups(
    daily_data: dict[str, dict],
    condition_key: str,
    target_key: str,
) -> dict | None:
    with_condition = []
    without_condition = []

    for _, data in daily_data.items():
        target_value = data.get(target_key)
        condition_value = data.get(condition_key)

        if target_value is None:
            continue

        if condition_value == 1:
            with_condition.append(float(target_value))
        else:
            without_condition.append(float(target_value))

    if len(with_condition) < 2 or len(without_condition) < 2:
        return None

    avg_with = safe_mean(with_condition)
    avg_without = safe_mean(without_condition)
    diff = avg_with - avg_without

    return {
        "with_count": len(with_condition),
        "without_count": len(without_condition),
        "avg_with": avg_with,
        "avg_without": avg_without,
        "diff": diff,
    }


def confidence_from_counts(n1: int, n2: int, effect_size: float, scale: float = 1.0) -> float:
    base = min((n1 + n2) / 10, 1.0) * 0.5
    effect = min(abs(effect_size) / scale, 1.0) * 0.5
    return round(min(base + effect, 0.95), 2)


def analyze_daily_correlations(daily_data: dict[str, dict]) -> tuple[list[dict], list[dict]]:
    insights = []
    recommendations = []

    result = compare_groups(daily_data, "late_caffeine", "sleep_duration_hours")
    if result and result["diff"] < -0.5:
        confidence = confidence_from_counts(result["with_count"], result["without_count"], result["diff"], scale=1.5)
        recommendations.append(build_recommendation(
            category="correlation",
            title="Снизить кофеин во второй половине дня",
            description="По вашим данным поздний кофеин связан с более коротким сном.",
            priority="high",
            confidence=confidence,
            action="Исключите кофеин после 15:00–16:00 и сравните сон в течение недели.",
            why_this="Группы дней с поздним кофеином и без него заметно различаются по длительности сна.",
            based_on=(
                f"С кофеином поздно: {result['avg_with']:.1f} ч сна, "
                f"без него: {result['avg_without']:.1f} ч."
            ),
            expected_effect="Устранение позднего кофеина должно увеличить длительность и стабильность сна."
        ))

    result = compare_groups(daily_data, "late_meal", "sleep_quality_score")
    if result and result["diff"] < -8:
        confidence = confidence_from_counts(result["with_count"], result["without_count"], result["diff"], scale=20)
        recommendations.append(build_recommendation(
            category="correlation",
            title="Сместить ужин раньше",
            description="По вашим данным поздняя еда связана с более низким качеством сна.",
            priority="high",
            confidence=confidence,
            action="Старайтесь завершать ужин за 2–3 часа до сна.",
            why_this="В дни с поздней едой показатели сна ниже, чем в дни без нее.",
            based_on=(
                f"С поздней едой: {result['avg_with']:.0f}, "
                f"без поздней еды: {result['avg_without']:.0f}."
            ),
            expected_effect="Более ранний ужин должен улучшить сон и уменьшить тяжесть перед засыпанием."
        ))

    result = compare_groups(daily_data, "evening_walk", "sleep_latency_minutes")
    if result and result["diff"] < -8:
        confidence = confidence_from_counts(result["with_count"], result["without_count"], result["diff"], scale=25)
        recommendations.append(build_recommendation(
            category="correlation",
            title="Использовать вечернюю прогулку как рабочую привычку",
            description="По вашим данным спокойная прогулка вечером связана с более быстрым засыпанием.",
            priority="low",
            confidence=confidence,
            action="Продолжайте 20–30 минут спокойной ходьбы вечером.",
            why_this="В дни с вечерней прогулкой вы засыпаете быстрее.",
            based_on=(
                f"С прогулкой: {result['avg_with']:.0f} мин до сна, "
                f"без прогулки: {result['avg_without']:.0f} мин."
            ),
            expected_effect="Поддержание прогулок поможет расслаблению и снижению вечернего напряжения."
        ))

    result = compare_groups(daily_data, "evening_high_activity", "sleep_quality_score")
    if result and result["diff"] < -8:
        confidence = confidence_from_counts(result["with_count"], result["without_count"], result["diff"], scale=20)
        recommendations.append(build_recommendation(
            category="correlation",
            title="Смещать интенсивные тренировки раньше",
            description="По вашим данным поздняя интенсивная активность связана с худшим сном.",
            priority="high",
            confidence=confidence,
            action="Перенесите интенсивные тренировки минимум на 3 часа раньше сна.",
            why_this="Поздняя интенсивная активность совпадает с более низким качеством сна.",
            based_on=(
                f"С поздней интенсивной активностью: {result['avg_with']:.0f}, "
                f"без нее: {result['avg_without']:.0f}."
            ),
            expected_effect="Более раннее завершение тренировки должно облегчить засыпание."
        ))

    extended_daily = {}
    for day_key, day_data in daily_data.items():
        extended_daily[day_key] = {
            **day_data,
            "low_hydration": 1 if day_data.get("hydration_effective_ml", 0) < 1500 else 0
        }

    result = compare_groups(extended_daily, "low_hydration", "energy")
    if result and result["diff"] < -1.0:
        confidence = confidence_from_counts(result["with_count"], result["without_count"], result["diff"], scale=3)
        recommendations.append(build_recommendation(
            category="correlation",
            title="Поднять уровень воды для улучшения энергии",
            description="По вашим данным в дни с низкой гидратацией уровень энергии ниже.",
            priority="medium",
            confidence=confidence,
            action="Добавьте 400–600 мл воды в первую половину дня и отслеживайте энергию.",
            why_this="Низкая гидратация у вас сочетается с более слабой энергией.",
            based_on=(
                f"При низкой воде энергия: {result['avg_with']:.1f}, "
                f"при нормальной воде: {result['avg_without']:.1f}."
            ),
            expected_effect="Рост гидратации должен помочь удерживать энергию в течение дня."
        ))

    return insights, recommendations


def analyze_user(db: Session, user_id: int, period_days: int = 14) -> dict:
    now = datetime.now(timezone.utc)
    period_start = now - timedelta(days=period_days)

    sleeps = (
        db.query(SleepRecord)
        .filter(SleepRecord.user_id == user_id, SleepRecord.sleep_start >= period_start)
        .all()
    )

    meals = (
        db.query(MealRecord)
        .filter(MealRecord.user_id == user_id, MealRecord.meal_time >= period_start)
        .all()
    )

    hydration = (
        db.query(HydrationRecord)
        .filter(HydrationRecord.user_id == user_id, HydrationRecord.record_time >= period_start)
        .all()
    )

    activities = (
        db.query(ActivityRecord)
        .filter(ActivityRecord.user_id == user_id, ActivityRecord.start_time >= period_start)
        .all()
    )

    states = (
        db.query(UserState)
        .filter(UserState.user_id == user_id, UserState.record_time >= period_start)
        .all()
    )

    sleep_insights, sleep_recommendations, sleep_score = analyze_sleep(sleeps)
    meal_insights, meal_recommendations, nutrition_score = analyze_meals(meals)
    hydration_insights, hydration_recommendations, hydration_score = analyze_hydration(hydration)
    activity_insights, activity_recommendations, activity_score = analyze_activity(activities)
    state_insights, state_recommendations, state_score = analyze_state(states)

    daily_data = aggregate_daily_data(
        sleeps=sleeps,
        meals=meals,
        hydration=hydration,
        activities=activities,
        states=states,
    )

    correlation_insights, correlation_recommendations = analyze_daily_correlations(daily_data)

    all_insights = (
        sleep_insights +
        meal_insights +
        hydration_insights +
        activity_insights +
        state_insights +
        correlation_insights
    )

    all_recommendations = (
        sleep_recommendations +
        meal_recommendations +
        hydration_recommendations +
        activity_recommendations +
        state_recommendations +
        correlation_recommendations
    )

    all_insights = deduplicate_insights(all_insights)
    all_recommendations = deduplicate_recommendations(all_recommendations)
    all_recommendations = sort_recommendations(all_recommendations)

    health_score = calculate_health_score(
        sleep_score=sleep_score,
        hydration_score=hydration_score,
        activity_score=activity_score,
        nutrition_score=nutrition_score,
        state_score=state_score,
    )

    return {
        "summary": {
            "period_days": period_days,
            "health_score": health_score,
            "sleep_score": sleep_score,
            "hydration_score": hydration_score,
            "activity_score": activity_score,
            "nutrition_score": nutrition_score,
            "state_score": state_score,
        },
        "insights": all_insights,
        "recommendations": all_recommendations,
    }


def save_analysis_result(db: Session, user_id: int, analytics_result: dict) -> AnalysisRun:
    summary = analytics_result["summary"]
    insights = analytics_result["insights"]
    recommendations = analytics_result["recommendations"]

    analysis_run = AnalysisRun(
        user_id=user_id,
        period_days=summary["period_days"],
        health_score=summary["health_score"],
        sleep_score=summary["sleep_score"],
        hydration_score=summary["hydration_score"],
        activity_score=summary["activity_score"],
        nutrition_score=summary["nutrition_score"],
        state_score=summary["state_score"],
    )
    db.add(analysis_run)
    db.commit()
    db.refresh(analysis_run)

    for item in insights:
        saved_item = SavedRecommendation(
            analysis_run_id=analysis_run.id,
            user_id=user_id,
            item_type="insight",
            category=item["category"],
            title=item["title"],
            description=item["description"],
            impact=item["impact"],
            confidence=item["confidence"],
            priority=None,
            action=None,
            why_this=item.get("why_this"),
            based_on=item.get("based_on"),
            expected_effect=item.get("expected_effect"),
            status="new",
            is_read=False,
            is_active=True,
        )
        db.add(saved_item)

    for item in recommendations:
        saved_item = SavedRecommendation(
            analysis_run_id=analysis_run.id,
            user_id=user_id,
            item_type="recommendation",
            category=item["category"],
            title=item["title"],
            description=item["description"],
            priority=item["priority"],
            impact=None,
            confidence=item["confidence"],
            action=item.get("action"),
            why_this=item.get("why_this"),
            based_on=item.get("based_on"),
            expected_effect=item.get("expected_effect"),
            status="new",
            is_read=False,
            is_active=True,
        )
        db.add(saved_item)

    db.commit()
    db.refresh(analysis_run)

    action_plans = build_action_plan_from_recommendations(
        analysis_run_id=analysis_run.id,
        user_id=user_id,
        recommendations=recommendations,
    )

    for plan in action_plans:
        db.add(plan)

    db.commit()
    return analysis_run