from datetime import date, datetime, time

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.meal import MealRecord
from app.models.saved_dish import SavedDish
from app.models.user import User
from app.schemas.meal import MealCreate, MealOut
from app.schemas.meal_copy import CopyDayRequest
from app.schemas.saved_dish import SavedDishCreate, SavedDishOut
from app.services.analytics_sync import rebuild_user_analytics
from app.services.realtime_manager import realtime_manager
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/meal", tags=["Meal"])


def _day_bounds(d: date) -> tuple[datetime, datetime]:
    start = datetime.combine(d, time.min)
    end = datetime.combine(d, time.max)
    return start, end


@router.get("/today", response_model=MealOut | None)
def get_today_meal(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    today = datetime.now().date()
    day_start, day_end = _day_bounds(today)

    meal = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == current_user.id,
            MealRecord.meal_time >= day_start,
            MealRecord.meal_time <= day_end,
        )
        .order_by(MealRecord.meal_time.desc())
        .first()
    )

    return meal


@router.get("/history", response_model=list[MealOut])
def get_meal_history(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    meals = (
        db.query(MealRecord)
        .filter(MealRecord.user_id == current_user.id)
        .order_by(MealRecord.meal_time.desc())
        .all()
    )
    return meals


@router.get("/saved", response_model=list[SavedDishOut])
def list_saved_dishes(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    return (
        db.query(SavedDish)
        .filter(SavedDish.user_id == current_user.id)
        .order_by(SavedDish.created_at.desc())
        .all()
    )


@router.post("/saved", response_model=SavedDishOut)
def create_saved_dish(
    body: SavedDishCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    row = SavedDish(
        user_id=current_user.id,
        name=body.name.strip(),
        meal_type=body.meal_type,
        calories=body.calories,
        protein_g=body.protein_g,
        fat_g=body.fat_g,
        carbs_g=body.carbs_g,
        notes=body.notes,
    )
    db.add(row)
    db.commit()
    db.refresh(row)
    return row


@router.delete("/saved/{saved_id}")
def delete_saved_dish(
    saved_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    row = (
        db.query(SavedDish)
        .filter(SavedDish.id == saved_id, SavedDish.user_id == current_user.id)
        .first()
    )
    if not row:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Сохранённое блюдо не найдено")
    db.delete(row)
    db.commit()
    return {"message": "Удалено"}


@router.post("/copy-day", response_model=dict)
def copy_meals_from_day(
    body: CopyDayRequest,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    target = body.target_date or datetime.now().date()
    src_start, src_end = _day_bounds(body.source_date)
    meals = (
        db.query(MealRecord)
        .filter(
            MealRecord.user_id == current_user.id,
            MealRecord.meal_time >= src_start,
            MealRecord.meal_time <= src_end,
        )
        .order_by(MealRecord.meal_time.asc())
        .all()
    )
    if not meals:
        return {"copied": 0, "message": "На исходную дату нет записей"}

    copied = 0
    for m in meals:
        t_part = m.meal_time.time()
        new_dt = datetime.combine(target, t_part)
        if m.meal_time.tzinfo is not None:
            new_dt = new_dt.replace(tzinfo=m.meal_time.tzinfo)
        new = MealRecord(
            user_id=current_user.id,
            meal_type=m.meal_type,
            name=m.name,
            calories=m.calories,
            protein_g=m.protein_g,
            fat_g=m.fat_g,
            carbs_g=m.carbs_g,
            fiber_g=m.fiber_g,
            sugar_g=m.sugar_g,
            caffeine_mg=m.caffeine_mg,
            water_ml=m.water_ml,
            portion_g=m.portion_g,
            glycemic_load=m.glycemic_load,
            meal_category=m.meal_category,
            minutes_before_sleep=m.minutes_before_sleep,
            is_late_meal=m.is_late_meal,
            meal_time=new_dt,
            notes=m.notes,
            source="copy_day",
        )
        db.add(new)
        copied += 1
    db.commit()

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "meal",
        "Скопированы приёмы пищи",
    )

    return {"copied": copied, "target_date": str(target)}


@router.get("/{meal_id}", response_model=MealOut)
def get_meal_by_id(
    meal_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    meal = (
        db.query(MealRecord)
        .filter(
            MealRecord.id == meal_id,
            MealRecord.user_id == current_user.id,
        )
        .first()
    )
    if not meal:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись питания не найдена")
    return meal


@router.put("/{meal_id}", response_model=MealOut)
def update_meal(
    meal_id: int,
    data: MealCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    meal = (
        db.query(MealRecord)
        .filter(
            MealRecord.id == meal_id,
            MealRecord.user_id == current_user.id,
        )
        .first()
    )
    if not meal:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись питания не найдена")

    meal.meal_type = data.meal_type
    meal.name = data.name
    meal.calories = data.calories
    meal.protein_g = data.protein_g
    meal.fat_g = data.fat_g
    meal.carbs_g = data.carbs_g
    meal.fiber_g = data.fiber_g
    meal.sugar_g = data.sugar_g
    meal.caffeine_mg = data.caffeine_mg
    meal.water_ml = data.water_ml
    meal.portion_g = data.portion_g
    meal.glycemic_load = data.glycemic_load
    meal.meal_category = data.meal_category
    meal.minutes_before_sleep = data.minutes_before_sleep
    meal.is_late_meal = data.is_late_meal
    meal.meal_time = data.meal_time
    meal.notes = data.notes
    meal.source = data.source

    db.add(meal)
    db.commit()
    db.refresh(meal)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "meal",
        "Обновлены данные питания",
    )

    return meal


@router.delete("/{meal_id}")
def delete_meal(
    meal_id: int,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    meal = (
        db.query(MealRecord)
        .filter(
            MealRecord.id == meal_id,
            MealRecord.user_id == current_user.id,
        )
        .first()
    )
    if not meal:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Запись питания не найдена")

    db.delete(meal)
    db.commit()

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "meal",
        "Удалена запись питания",
    )

    return {"message": "Запись питания удалена"}


@router.post("/", response_model=MealOut)
def create_meal(
    data: MealCreate,
    background_tasks: BackgroundTasks,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    meal = MealRecord(
        user_id=current_user.id,
        meal_type=data.meal_type,
        name=data.name,
        calories=data.calories,
        protein_g=data.protein_g,
        fat_g=data.fat_g,
        carbs_g=data.carbs_g,
        fiber_g=data.fiber_g,
        sugar_g=data.sugar_g,
        caffeine_mg=data.caffeine_mg,
        water_ml=data.water_ml,
        portion_g=data.portion_g,
        glycemic_load=data.glycemic_load,
        meal_category=data.meal_category,
        minutes_before_sleep=data.minutes_before_sleep,
        is_late_meal=data.is_late_meal,
        meal_time=data.meal_time,
        notes=data.notes,
        source=data.source,
    )
    db.add(meal)
    db.commit()
    db.refresh(meal)

    rebuild_user_analytics(db, current_user.id, days=7)
    generate_smart_triggers_and_reminders(db, current_user.id, period_days=7)

    background_tasks.add_task(
        realtime_manager.broadcast_user_update,
        current_user.id,
        "meal",
        "Обновлены данные питания",
    )

    return meal
