from pydantic import BaseModel, Field

from app.schemas.activity import ActivityCreate
from app.schemas.health_sample import HealthSampleCreate
from app.schemas.hydration import HydrationCreate
from app.schemas.meal import MealCreate
from app.schemas.sleep import SleepCreate


class ImportBatchRequest(BaseModel):
    hydration: list[HydrationCreate] = Field(default_factory=list)
    meals: list[MealCreate] = Field(default_factory=list)
    sleeps: list[SleepCreate] = Field(default_factory=list)
    activities: list[ActivityCreate] = Field(default_factory=list)
    health_samples: list[HealthSampleCreate] = Field(default_factory=list)


class ImportBatchResponse(BaseModel):
    hydration_created: int = 0
    meals_created: int = 0
    sleeps_created: int = 0
    activities_created: int = 0
    health_samples_created: int = 0
    errors: list[str] = Field(default_factory=list)


class CsvImportBody(BaseModel):
    text: str = Field(..., description="Текст CSV: по строке на запись, поля через «;»")
