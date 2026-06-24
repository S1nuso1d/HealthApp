from app.models.profile import UserProfile
from app.models.user import User
from app.services.profile_display import public_display_name


def user_matches_search_query(user: User, profile: UserProfile | None, query: str) -> bool:
    needle = query.strip().casefold()
    if len(needle) < 2:
        return False

    email = (user.email or "").casefold()
    nick = (profile.nickname or "").strip().casefold() if profile else ""
    first = (profile.first_name or "").strip().casefold() if profile else ""
    last = (profile.last_name or "").strip().casefold() if profile else ""
    full = f"{first} {last}".strip()
    display = public_display_name(profile, user).casefold()
    fields = [email, nick, first, last, full, display]

    if any(needle in field for field in fields if field):
        return True

    tokens = [t for t in needle.split() if t]
    if len(tokens) >= 2:
        if tokens[0] in first and tokens[1] in last:
            return True
        if tokens[0] in last and tokens[1] in first:
            return True

    if len(tokens) == 1:
        token = tokens[0]
        return any(token in field for field in fields if field)

    return False
