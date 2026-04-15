from fastapi import FastAPI

from app.db.database import Base, engine
from app.models import (
    user,
    profile,
    sleep,
    meal,
    hydration,
    activity,
    user_state,
    analysis_run,
    saved_recommendation,
    action_plan,
    smart_trigger,
    smart_reminder,
    daily_health_summary,
    insight,
)
from app.api.auth import router as auth_router
from app.api.profile import router as profile_router
from app.api.sleep import router as sleep_router
from app.api.meal import router as meal_router
from app.api.hydration import router as hydration_router
from app.api.activity import router as activity_router
from app.api.user_state import router as user_state_router
from app.api.analytics import router as analytics_router
from app.api.smart_triggers import router as smart_triggers_router
from app.api.action_plan import router as action_plan_router
from app.api.ai import router as ai_router
from app.api.dashboard import router as dashboard_router
from app.api import activity, meal

Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="HealthApp API",
    description="API для мобильного приложения персонального помощника по здоровью",
    version="1.1.1",
)

app.include_router(auth_router)
app.include_router(profile_router)
app.include_router(sleep_router)
app.include_router(meal_router)
app.include_router(hydration_router)
app.include_router(activity_router)
app.include_router(user_state_router)
app.include_router(analytics_router)
app.include_router(smart_triggers_router)
app.include_router(action_plan_router)
app.include_router(ai_router)
app.include_router(dashboard_router)

@app.get("/", summary="Проверка работы API")
def root():
    return {"message": "HealthApp API is running"}