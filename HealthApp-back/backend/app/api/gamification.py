from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.user import User
from app.services.achievement_service import list_achievements

router = APIRouter(prefix="/achievements", tags=["Achievements"])


@router.get("/me")
def get_my_achievements(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return list_achievements(db, current_user.id)
