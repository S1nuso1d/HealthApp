from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.core.config import settings

from app.api.auth import router as auth_router
from app.api.profile import router as profile_router
from app.api.sleep import router as sleep_router
from app.api.hydration import router as hydration_router
from app.api.meal import router as meal_router
from app.api.activity import router as activity_router
from app.api.states import router as states_router
from app.api.ai import router as ai_router
from app.api.analytics import router as analytics_router
from app.api.smart import router as smart_router
from app.api.action_plan import router as action_plan_router
from app.api.dashboard import router as dashboard_router
from app.api.ws import router as ws_router

app = FastAPI(title=settings.PROJECT_NAME)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    # With wildcard origins, credentials must be disabled (browsers will reject otherwise).
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth_router)
app.include_router(profile_router)
app.include_router(sleep_router)
app.include_router(hydration_router)
app.include_router(meal_router)
app.include_router(activity_router)
app.include_router(states_router)
app.include_router(ai_router)
app.include_router(analytics_router)
app.include_router(smart_router)
app.include_router(action_plan_router)
app.include_router(dashboard_router)
app.include_router(ws_router)


@app.get("/")
def root():
    return {"message": "HealthApp API is running"}