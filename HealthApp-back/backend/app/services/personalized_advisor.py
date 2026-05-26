"""
Персональные рекомендации с опорой на факты из дневника пользователя.
Не шаблонные «пейте воду» — конкретные события, сравнение с привычным уровнем, связь с последствиями.
"""

from __future__ import annotations

import re
from collections import defaultdict
from datetime import datetime, timedelta, timezone
from statistics import mean, pstdev
from typing import Any

from sqlalchemy.orm import Session

from app.models.activity import ActivityRecord
from app.models.hydration import HydrationRecord
from app.models.meal import MealRecord
from app.models.profile import UserProfile
from app.models.sleep import SleepRecord
from app.services.analytics.analytics_service import build_recommendation, deduplicate_recommendations, sort_recommendations
from app.services.meal_timing import enrich_meal_timing, infer_is_late_meal


def _aware(dt: datetime) -> datetime:
    if dt.tzinfo is None:
        return dt.replace(tzinfo=timezone.utc)
    return dt


def _fmt_date(dt: datetime) -> str:
    return _aware(dt).strftime("%d.%m")


def _fmt_time(dt: datetime) -> str:
    return _aware(dt).strftime("%H:%M")


def _meal_is_late(m: MealRecord) -> bool:
    if m.is_late_meal in (1, True):
        return True
    if m.minutes_before_sleep is not None and m.minutes_before_sleep < 180:
        return True
    return infer_is_late_meal(m.meal_time, m.minutes_before_sleep)


def _meal_is_late_caffeine(m: MealRecord) -> bool:
    if (m.caffeine_mg or 0) < 40:
        return False
    if m.minutes_before_sleep is not None and m.minutes_before_sleep < 360:
        return True
    return _aware(m.meal_time).hour >= 16


def _sleep_after_meal(meal: MealRecord, sleeps: list[SleepRecord]) -> SleepRecord | None:
    meal_t = _aware(meal.meal_time)
    candidates = [
        s for s in sleeps
        if _aware(s.sleep_start) > meal_t
        and (_aware(s.sleep_start) - meal_t).total_seconds() < 36 * 3600
    ]
    if not candidates:
        return None
    return min(candidates, key=lambda s: _aware(s.sleep_start))


def _baseline_sleep(sleeps: list[SleepRecord]) -> tuple[float, float]:
    durations = [float(s.duration_hours or 0) for s in sleeps if (s.duration_hours or 0) > 0]
    qualities = [float(s.quality_score or 0) for s in sleeps if (s.quality_score or 0) > 0]
    return (
        mean(durations) if durations else 0.0,
        mean(qualities) if qualities else 0.0,
    )


class PersonalizedAdvisor:
    @staticmethod
    def generate_recommendations(
        db: Session,
        user_id: int,
        period_days: int = 14,
    ) -> list[dict[str, Any]]:
        now = datetime.now(timezone.utc)
        period_start = now - timedelta(days=period_days)

        profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
        target_water = int(profile.target_water_ml if profile and profile.target_water_ml else 2500)
        target_sleep = float(profile.target_sleep_hours if profile and profile.target_sleep_hours else 8.0)
        target_steps = int(profile.target_steps if profile and profile.target_steps else 10_000)

        sleeps = (
            db.query(SleepRecord)
            .filter(SleepRecord.user_id == user_id, SleepRecord.sleep_end >= period_start)
            .order_by(SleepRecord.sleep_end.desc())
            .all()
        )
        meals = (
            db.query(MealRecord)
            .filter(MealRecord.user_id == user_id, MealRecord.meal_time >= period_start)
            .order_by(MealRecord.meal_time.desc())
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
            .order_by(ActivityRecord.start_time.desc())
            .all()
        )

        items: list[dict[str, Any]] = []
        avg_dur, avg_qual = _baseline_sleep(sleeps)

        # --- Поздний кофеин + ухудшение сна той же ночи ---
        for meal in meals:
            if not _meal_is_late_caffeine(meal):
                continue
            night = _sleep_after_meal(meal, sleeps)
            if night is None or avg_dur <= 0:
                continue
            dur = float(night.duration_hours or 0)
            qual = float(night.quality_score or 0)
            worse_dur = dur < avg_dur - 0.45
            worse_qual = avg_qual > 0 and qual > 0 and qual < avg_qual - 8
            if not worse_dur and not worse_qual:
                continue
            caff = int(meal.caffeine_mg or 0)
            parts = [
                f"{_fmt_date(meal.meal_time)} в {_fmt_time(meal.meal_time)} — «{meal.name}» ({caff} мг кофеина).",
            ]
            if worse_dur:
                parts.append(
                    f"В эту ночь сон {dur:.1f} ч (ваш обычный уровень ~{avg_dur:.1f} ч)."
                )
            if worse_qual:
                parts.append(
                    f"Качество сна {qual:.0f}% (обычно ~{avg_qual:.0f}%)."
                )
            items.append(
                build_recommendation(
                    category="meals",
                    title="Поздний кофеин мог ухудшить ваш сон",
                    description=" ".join(parts),
                    priority="high",
                    confidence=0.9,
                    action="3–5 дней без кофеина после 15:00 — проверьте, станет ли засыпание легче.",
                    why_this="Связь между конкретным приёмом с кофеином и более слабой ночью в вашем дневнике.",
                    based_on=f"Запись {meal.id}, сон после { _fmt_date(night.sleep_end)}.",
                    expected_effect="Ранний отказ от кофеина часто улучшает глубину сна и время засыпания.",
                )
            )
            if len([i for i in items if "кофеин" in i["title"].lower()]) >= 2:
                break

        # --- Поздний ужин / еда перед сном ---
        late_meal_events = [m for m in meals if _meal_is_late(m)][:5]
        if len(late_meal_events) >= 2:
            examples = []
            for m in late_meal_events[:3]:
                line = f"{_fmt_date(m.meal_time)} {_fmt_time(m.meal_time)} — «{m.name}»"
                if m.minutes_before_sleep is not None and m.minutes_before_sleep < 180:
                    line += f", за {m.minutes_before_sleep} мин до сна"
                examples.append(line)
            items.append(
                build_recommendation(
                    category="meals",
                    title="Вы часто едите слишком поздно",
                    description=(
                        f"За {period_days} дн. зафиксировано {len(late_meal_events)} приёмов пищи "
                        f"поздно вечером или менее чем за 3 ч до сна. Например: "
                        + "; ".join(examples) + "."
                    ),
                    priority="high",
                    confidence=0.88,
                    action="Перенесите плотный ужин на 19:00–20:00, после 21:00 — только лёгкий перекус при необходимости.",
                    why_this="Поздняя еда в вашем дневнике повторяется и может мешать сну.",
                    based_on=f"{len(late_meal_events)} поздних записей за период.",
                    expected_effect="Более ранний ужин обычно облегчает засыпание и снижает тяжесть утром.",
                )
            )

        for meal in late_meal_events:
            night = _sleep_after_meal(meal, sleeps)
            if night is None or avg_qual <= 0:
                continue
            qual = float(night.quality_score or 0)
            if qual >= avg_qual - 5:
                continue
            items.append(
                build_recommendation(
                    category="correlation",
                    title="Ужин перед сном совпал со слабым сном",
                    description=(
                        f"{_fmt_date(meal.meal_time)} поздний приём «{meal.name}». "
                        f"В эту ночь качество сна {qual:.0f}% при вашем среднем ~{avg_qual:.0f}%."
                    ),
                    priority="high",
                    confidence=0.85,
                    action="В день, когда ложитесь раньше, завершайте еду минимум за 2,5–3 часа.",
                    why_this="Конкретная ночь после поздней еды выглядит хуже вашей нормы.",
                    based_on=f"Приём пищи + сон {_fmt_date(night.sleep_end)}.",
                    expected_effect="Разрыв между ужином и сном снижает нагрузку на пищеварение и облегчает засыпание.",
                )
            )
            break

        # --- Мало воды (сегодня и среднее) ---
        daily_water: dict[str, float] = defaultdict(float)
        for h in hydration:
            day = _aware(h.record_time).date().isoformat()
            factor = float(h.hydration_factor) if h.hydration_factor is not None else 1.0
            daily_water[day] += float(h.amount_ml or 0) * factor

        today_key = now.date().isoformat()
        today_ml = int(daily_water.get(today_key, 0))
        if today_ml > 0 and today_ml < target_water * 0.45:
            items.append(
                build_recommendation(
                    category="hydration",
                    title="Сегодня воды заметно меньше вашей цели",
                    description=(
                        f"Сегодня в дневнике {today_ml} мл при цели {target_water} мл "
                        f"({today_ml * 100 // max(target_water, 1)}%). Недобор часто даёт усталость и головную тяжесть к вечеру."
                    ),
                    priority="high",
                    confidence=0.92,
                    action=f"Добавьте ещё {max(target_water - today_ml, 250)} мл до конца дня небольшими порциями.",
                    why_this="Ваши сегодняшние записи по воде ниже персональной цели из профиля.",
                    based_on=f"Сумма за {today_key}: {today_ml} мл.",
                    expected_effect="Добор воды в течение дня обычно улучшает концентрацию и самочувствие.",
                )
            )

        if daily_water:
            avg_w = mean(daily_water.values())
            if avg_w < target_water * 0.65 and len(daily_water) >= 3:
                low_days = sum(1 for v in daily_water.values() if v < target_water * 0.6)
                items.append(
                    build_recommendation(
                        category="hydration",
                        title="В среднем вы пьёте меньше, чем задумывали",
                        description=(
                            f"За {len(daily_water)} дн. с записями в среднем {avg_w:.0f} мл/день "
                            f"при цели {target_water} мл. Слабые дни: {low_days} из {len(daily_water)}."
                        ),
                        priority="high",
                        confidence=0.86,
                        action="Поставьте напоминание каждые 2 ч и записывайте стакан сразу после приёма.",
                        why_this="Хронический недобор воды в вашем дневнике.",
                        based_on=f"Среднее {avg_w:.0f} мл/день за период.",
                        expected_effect="Стабильная гидратация снижает скачки усталости.",
                    )
                )

        # --- Тяжёлая тренировка вечером ---
        evening_hard = [
            a for a in activities
            if (a.is_evening_activity in (1, True) or _aware(a.start_time).hour >= 19)
            and (a.intensity or "").lower() in ("high", "высокая")
        ]
        if len(evening_hard) >= 1:
            a = evening_hard[0]
            label = a.activity_type or "тренировка"
            items.append(
                build_recommendation(
                    category="activity",
                    title="Тяжёлая нагрузка вечером мешает отдыху",
                    description=(
                        f"{_fmt_date(a.start_time)} в {_fmt_time(a.start_time)} — {label} "
                        f"({a.duration_minutes or 0} мин, высокая интенсивность). "
                        "Такие сессии близко ко сну возбуждают нервную систему и отодвигают засыпание."
                    ),
                    priority="high",
                    confidence=0.87,
                    action="Перенесите силовые и HIIT на утро или до 17:00; вечером — прогулка или растяжка.",
                    why_this="В дневнике есть интенсивная вечерняя активность.",
                    based_on=f"Запись активности {a.id}.",
                    expected_effect="Более спокойный вечер обычно ускоряет засыпание.",
                )
            )

        # --- Нерегулярное время отхода ко сну ---
        if len(sleeps) >= 4:
            start_hours = [_aware(s.sleep_start).hour + _aware(s.sleep_start).minute / 60.0 for s in sleeps]
            spread = pstdev(start_hours) if len(start_hours) >= 2 else 0.0
            if spread >= 1.2:
                earliest = min(start_hours)
                latest = max(start_hours)
                items.append(
                    build_recommendation(
                        category="sleep",
                        title="Время отхода ко сну сильно «плавает»",
                        description=(
                            f"За {len(sleeps)} ночей разброс отбоя ~{spread:.1f} ч "
                            f"(от {int(earliest)}:{int((earliest % 1) * 60):02d} до {int(latest)}:{int((latest % 1) * 60):02d}). "
                            "Нерегулярный ритм сбивает циркадные часы и ухудшает восстановление даже при нормальной длительности сна."
                        ),
                        priority="high",
                        confidence=0.84,
                        action="Выберите целевое время сна ±30 мин и придерживайтесь его 5–7 дней подряд, в том числе в выходные.",
                        why_this="Высокая вариативность времени засыпания в ваших записях.",
                        based_on=f"Отклонение отбоя: {spread:.1f} ч.",
                        expected_effect="Стабильный отбой обычно повышает качество сна и утреннюю бодрость.",
                    )
                )

        # --- Недосып ---
        recent = sleeps[:5]
        short_nights = [s for s in recent if (s.duration_hours or 0) < target_sleep - 0.75]
        if len(short_nights) >= 2:
            nights_txt = ", ".join(
                f"{_fmt_date(s.sleep_end)} ({s.duration_hours:.1f} ч)" for s in short_nights[:3]
            )
            items.append(
                build_recommendation(
                    category="sleep",
                    title="Несколько ночей подряд ниже вашей цели по сну",
                    description=(
                        f"Цель в профиле — {target_sleep:.1f} ч. Недавно короткие ночи: {nights_txt}. "
                        "Накопленный недосып снижает концентрацию и тянет к позднему кофеину и перееданию."
                    ),
                    priority="high",
                    confidence=0.9,
                    action="Сегодня заложите отбой на 45–60 мин раньше обычного и уберите экраны за час до сна.",
                    why_this="Повторяющийся недосып в ваших последних записях.",
                    based_on=f"{len(short_nights)} коротких ночей из {len(recent)} последних.",
                    expected_effect="Восстановление длительности сна улучшает энергию и самочувствие.",
                )
            )

        # --- Мало клетчатки / овощей (по fiber_g) ---
        daily_fiber: dict[str, float] = defaultdict(float)
        for m in meals:
            day = _aware(m.meal_time).date().isoformat()
            daily_fiber[day] += float(m.fiber_g or 0)
        if len(daily_fiber) >= 3:
            avg_fiber = mean(daily_fiber.values())
            if avg_fiber < 18:
                items.append(
                    build_recommendation(
                        category="meals",
                        title="В рационе мало клетчатки (овощей и цельных продуктов)",
                        description=(
                            f"По вашим записям в среднем ~{avg_fiber:.0f} г клетчатки в день "
                            f"(ориентир 25–30 г). Это часто значит мало овощей, бобовых и цельных круп — "
                            "на фоне может расти тяга к перекусам и колебания энергии."
                        ),
                        priority="medium",
                        confidence=0.78,
                        action="Добавьте к каждому основному приёму овощи или салат (1–2 горсти) и отмечайте fiber в дневнике.",
                        why_this="Низкая сумма fiber_g по дням в дневнике.",
                        based_on=f"Среднее {avg_fiber:.1f} г/день за {len(daily_fiber)} дн.",
                        expected_effect="Больше клетчатки улучшает сытость и стабильность пищеварения.",
                    )
                )

        # --- Мало белка ---
        proteins = [float(m.protein_g or 0) for m in meals if (m.protein_g or 0) > 0]
        if len(proteins) >= 5:
            avg_p = mean(proteins)
            if avg_p < 18:
                items.append(
                    build_recommendation(
                        category="meals",
                        title="Белка на приём пищи обычно мало",
                        description=(
                            f"В среднем ~{avg_p:.0f} г белка на запись — для сытости и восстановления "
                            "часто нужно больше в основных приёмах."
                        ),
                        priority="medium",
                        confidence=0.72,
                        action="Добавьте источник белка (яйца, рыба, творог, бобовые) в завтрак и обед.",
                        why_this="Средний protein_g по вашим приёмам пищи низкий.",
                        based_on=f"Среднее {avg_p:.1f} г на запись.",
                        expected_effect="Достаточный белок помогает держать энергию и мышечное восстановление.",
                    )
                )

        # --- Шаги ---
        steps_by_day: dict[str, int] = defaultdict(int)
        for a in activities:
            day = _aware(a.start_time).date().isoformat()
            steps_by_day[day] += int(a.steps or 0)
        if steps_by_day:
            avg_steps = mean(steps_by_day.values())
            if avg_steps < target_steps * 0.55 and len(steps_by_day) >= 3:
                items.append(
                    build_recommendation(
                        category="activity",
                        title="Шаги стабильно ниже вашей цели",
                        description=(
                            f"В среднем ~{int(avg_steps)} шагов/день при цели {target_steps}. "
                            "Мало движения влияет на сон, настроение и расход калорий."
                        ),
                        priority="medium",
                        confidence=0.8,
                        action="Запланируйте 2 прогулки по 15–20 мин сегодня или поднимите лимит на 1000 шагов.",
                        why_this="Ваши записи активности ниже цели из профиля.",
                        based_on=f"Среднее за {len(steps_by_day)} дн.",
                        expected_effect="Регулярные шаги улучшают сон и метаболическое здоровье.",
                    )
                )

        return sort_recommendations(deduplicate_recommendations(items))

    @staticmethod
    def generate_insights(
        db: Session,
        user_id: int,
        period_days: int = 14,
    ) -> list[dict[str, Any]]:
        """Инсайты для таблицы insights (тот же анализ, другой формат)."""
        recs = PersonalizedAdvisor.generate_recommendations(db, user_id, period_days)
        insights = []
        for rec in recs:
            slug = re.sub(r"[^a-z0-9]+", "_", rec["title"].lower()).strip("_")[:48]
            desc = rec["description"]
            if rec.get("action"):
                desc = f"{desc}\n\nРекомендация: {rec['action']}"
            insights.append(
                {
                    "insight_type": f"personal_{rec['category']}_{slug}",
                    "category": rec["category"],
                    "title": rec["title"],
                    "description": desc,
                    "confidence": rec.get("confidence", 0.8),
                    "severity": "high" if rec.get("priority") == "high" else "medium",
                    "impact": "negative",
                    "window_days": period_days,
                }
            )
        return insights
