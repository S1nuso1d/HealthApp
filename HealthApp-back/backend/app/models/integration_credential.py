from sqlalchemy import Column, ForeignKey, Integer, String, Text
from sqlalchemy.orm import relationship

from app.db.database import Base


class IntegrationCredential(Base):
    """Токены сторонних API (например FatSecret OAuth 1.0), привязанные к пользователю."""

    __tablename__ = "integration_credentials"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    provider = Column(String, nullable=False, index=True)  # fatsecret
    access_token = Column(Text, nullable=False)
    access_secret = Column(Text, nullable=False)

    user = relationship("User")
