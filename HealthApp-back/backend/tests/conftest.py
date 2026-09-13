import uuid

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

import app.models  # noqa: F401 — регистрация моделей в metadata
from app.core.config import settings
from app.core.rate_limit import limiter
from app.db.database import Base, get_db
from app.main import app
from app.models.user import User

# Локальный backend/.env с реальным SMTP не должен ломать тесты (без исходящей почты).
settings.SMTP_HOST = ""
settings.SMTP_USER = ""
settings.SMTP_PASSWORD = ""


@pytest.fixture(autouse=True)
def reset_rate_limiter():
    """Тесты бьют по /auth пачками — счётчики не должны протекать между тестами.

    Тесты, которые проверяют сам лимитер, включают его через фикстуру `rate_limited`.
    """
    previous = settings.RATE_LIMIT_ENABLED
    settings.RATE_LIMIT_ENABLED = False
    limiter.reset()
    yield
    settings.RATE_LIMIT_ENABLED = previous
    limiter.reset()


@pytest.fixture
def rate_limited():
    """Включает ограничение частоты внутри одного теста."""
    settings.RATE_LIMIT_ENABLED = True
    limiter.reset()
    yield
    settings.RATE_LIMIT_ENABLED = False
    limiter.reset()


@pytest.fixture
def db_session():
    engine = create_engine(
        "sqlite://",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(bind=engine)
    TestSession = sessionmaker(bind=engine)
    session = TestSession()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture
def client(db_session):
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture
def test_user(db_session) -> User:
    """Пользователь прямо в БД, без прохода через HTTP-регистрацию."""
    user = User(
        email=f"svc_{uuid.uuid4().hex[:10]}@example.com",
        hashed_password="not-a-real-hash",
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    return user


@pytest.fixture
def auth_headers(client) -> dict[str, str]:
    from app.services.registration_email import LAST_SENT_CODES

    email = f"u_{uuid.uuid4().hex[:10]}@example.com"
    password = "testpassword123"
    resp = client.post(
        "/auth/register/start",
        json={"email": email, "password": password},
    )
    assert resp.status_code == 200, resp.text
    code = LAST_SENT_CODES[email.lower()]
    resp2 = client.post(
        "/auth/register/complete",
        json={"email": email, "password": password, "code": code},
    )
    assert resp2.status_code == 200, resp2.text
    token = resp2.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}
