from app.services.profile_display import validate_nickname, public_display_name
from app.models.profile import UserProfile
from app.models.user import User

def test_validate_nickname_valid():
    assert validate_nickname("john_doe123") == "john_doe123"
    assert validate_nickname("Ivan_Ivanov") == "Ivan_Ivanov"

def test_validate_nickname_invalid():
    assert validate_nickname("ab") is None # Too short
    assert validate_nickname("invalid name") is None # Has space
    assert validate_nickname("john@doe") is None # Special char

def test_public_display_name_with_nickname():
    user = User(email="test@example.com")
    profile = UserProfile(nickname="cool_user", first_name="Test", last_name="User")
    assert public_display_name(profile, user) == "cool_user"

def test_public_display_name_with_first_last_name():
    user = User(email="test@example.com")
    profile = UserProfile(first_name="Ivan", last_name="Ivanov")
    assert public_display_name(profile, user) == "Ivan Ivanov"

def test_public_display_name_with_email_fallback():
    user = User(email="testuser@example.com")
    profile = UserProfile()
    assert public_display_name(profile, user) == "testuser"

    user2 = User(email="t@example.com")
    assert public_display_name(profile, user2) == "t"
