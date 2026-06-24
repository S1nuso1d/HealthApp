from app.models.profile import UserProfile
from app.models.user import User
from app.services.user_search import user_matches_search_query


def test_search_by_cyrillic_first_name():
    user = User(id=2, email="other@mail.ru")
    profile = UserProfile(first_name="Данил", last_name="Татаринцев", nickname="Danil123")
    assert user_matches_search_query(user, profile, "данил")
    assert user_matches_search_query(user, profile, "ДАНИЛ")
    assert user_matches_search_query(user, profile, "татар")


def test_search_by_full_name():
    user = User(id=2, email="other@mail.ru")
    profile = UserProfile(first_name="Варвара", last_name="Обухова")
    assert user_matches_search_query(user, profile, "варвара обухова")
    assert user_matches_search_query(user, profile, "Обухова")


def test_search_by_nickname_and_email():
    user = User(id=2, email="test123@mail.ru")
    profile = UserProfile(nickname="Danil123")
    assert user_matches_search_query(user, profile, "danil123")
    assert user_matches_search_query(user, profile, "test123")
