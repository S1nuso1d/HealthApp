import pytest

from app.services.profile_display import validate_nickname, public_display_name
from app.models.profile import UserProfile
from app.models.user import User

def test_validate_nickname_valid():
    assert validate_nickname("john_doe123") == "john_doe123"
    assert validate_nickname("Ivan_Ivanov") == "Ivan_Ivanov"
    assert validate_nickname("Иван_Иванов") == "Иван_Иванов"

@pytest.mark.parametrize("value", ["ab", "invalid name", "john@doe", "a" * 33])
def test_validate_nickname_invalid_raises(value):
    """Неподходящий никнейм — это ошибка с объяснением, а не молчаливый None:
    иначе пользователь не узнает, почему поле не сохранилось."""
    with pytest.raises(ValueError):
        validate_nickname(value)

def test_validate_nickname_empty_is_none():
    assert validate_nickname(None) is None
    assert validate_nickname("   ") is None

def test_public_display_name_with_nickname():
    user = User(email="test@example.com")
    profile = UserProfile(nickname="cool_user", first_name="Test", last_name="User")
    assert public_display_name(profile, user) == "cool_user"

def test_public_display_name_with_first_last_name():
    user = User(email="test@example.com")
    profile = UserProfile(first_name="Ivan", last_name="Ivanov")
    assert public_display_name(profile, user) == "Ivan Ivanov"

def test_public_display_name_masks_email_fallback():
    """Без имени и никнейма показываем маскированный email — полный адрес утёк бы
    в общую ленту и поиск людей."""
    user = User(email="testuser@example.com")
    profile = UserProfile()
    assert public_display_name(profile, user) == "tes***@example.com"

    user2 = User(email="t@example.com")
    assert public_display_name(profile, user2) == "t***@example.com"
