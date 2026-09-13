from pydantic import BaseModel, ConfigDict, Field


class FoodCatalogItemOut(BaseModel):
    id: int | None = None
    barcode: str | None = None
    name: str
    brand: str | None = None
    calories_100g: float | None = None
    protein_g_100g: float | None = None
    fat_g_100g: float | None = None
    carbs_g_100g: float | None = None
    image_url: str | None = None
    source: str = "user"
    is_complete: bool = False
    needs_completion: bool = False

    model_config = ConfigDict(from_attributes=True)


class FoodCatalogSearchResponse(BaseModel):
    items: list[FoodCatalogItemOut] = Field(default_factory=list)


class FoodCatalogUpsertBody(BaseModel):
    barcode: str | None = None
    name: str | None = None
    brand: str | None = None
    calories_100g: float | None = None
    protein_g_100g: float | None = None
    fat_g_100g: float | None = None
    carbs_g_100g: float | None = None
    off_image_url: str | None = None
