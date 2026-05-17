from pydantic import BaseModel, Field


class FatSecretLinkBody(BaseModel):
    access_token: str = Field(..., description="OAuth resource owner key (FatSecret)")
    access_secret: str = Field(..., description="OAuth resource owner secret (FatSecret)")


class FatSecretSearchResponse(BaseModel):
    raw: dict = Field(description="Сырой JSON ответа FatSecret (format=json)")
