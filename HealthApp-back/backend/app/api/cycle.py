from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.user import User
from app.models.profile import UserProfile
from app.models.health import CycleEntry
from app.schemas.cycle import CycleEntryCreate, CycleEntryUpdate, CycleEntryResponse

router = APIRouter(prefix="/cycle", tags=["Cycle Tracking"])

def _is_female_sex(sex: str | None) -> bool:
    if not sex:
        return False
    normalized = sex.strip().lower()
    return normalized in {"female", "f", "ж", "женский", "woman", "women"}


def _check_female(db: Session, user_id: int):
    profile = db.query(UserProfile).filter(UserProfile.user_id == user_id).first()
    if not profile or not _is_female_sex(profile.sex):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Cycle tracking is only available for female users."
        )

@router.get("/", response_model=List[CycleEntryResponse])
def get_cycle_entries(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _check_female(db, current_user.id)
    entries = db.query(CycleEntry).filter(CycleEntry.user_id == current_user.id).order_by(CycleEntry.start_date.desc()).all()
    return entries

@router.post("/", response_model=CycleEntryResponse)
def create_cycle_entry(
    entry_in: CycleEntryCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _check_female(db, current_user.id)
    new_entry = CycleEntry(
        user_id=current_user.id,
        start_date=entry_in.start_date,
        end_date=entry_in.end_date,
        symptoms=entry_in.symptoms,
        notes=entry_in.notes
    )
    db.add(new_entry)
    db.commit()
    db.refresh(new_entry)
    return new_entry

@router.put("/{entry_id}", response_model=CycleEntryResponse)
def update_cycle_entry(
    entry_id: int,
    entry_in: CycleEntryUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _check_female(db, current_user.id)
    entry = db.query(CycleEntry).filter(CycleEntry.id == entry_id, CycleEntry.user_id == current_user.id).first()
    if not entry:
        raise HTTPException(status_code=404, detail="Cycle entry not found")
    
    if entry_in.start_date is not None:
        entry.start_date = entry_in.start_date
    if entry_in.end_date is not None:
        entry.end_date = entry_in.end_date
    if entry_in.symptoms is not None:
        entry.symptoms = entry_in.symptoms
    if entry_in.notes is not None:
        entry.notes = entry_in.notes
        
    db.commit()
    db.refresh(entry)
    return entry

@router.delete("/{entry_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_cycle_entry(
    entry_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    _check_female(db, current_user.id)
    entry = db.query(CycleEntry).filter(CycleEntry.id == entry_id, CycleEntry.user_id == current_user.id).first()
    if not entry:
        raise HTTPException(status_code=404, detail="Cycle entry not found")
    
    db.delete(entry)
    db.commit()
    return None
