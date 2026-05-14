from datetime import datetime, time

from fastapi import APIRouter, BackgroundTasks, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.meal import MealRecord
from app.models.user import User
from app.schemas.meal import MealCreate, MealOut
from app.services.analytics_sync import rebuild_user_analytics
from app.services.realtime_manager import realtime_manager
from app.services.smart_trigger_service import generate_smart_triggers_and_reminders

router = APIRouter(prefix="/meal", tags=["Meal"])


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


@router.get("/today", response_model=MealOut | None)
def get_today_meal(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    today = datetime.now().date()
    day_start = datetime.combine(today, time.min)
    day_end = datetime.combine(today, time.max)

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