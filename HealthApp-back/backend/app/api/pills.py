from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from typing import List

from app.api.deps import get_current_user
from app.db.database import get_db
from app.models.user import User
from app.models.health import PillReminder
from app.schemas.pills import PillReminderCreate, PillReminderUpdate, PillReminderOut

router = APIRouter(prefix="/pills", tags=["Pills"])

@router.get("/", response_model=List[PillReminderOut])
def get_pill_reminders(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    return db.query(PillReminder).filter(PillReminder.user_id == current_user.id).all()

@router.post("/", response_model=PillReminderOut, status_code=status.HTTP_201_CREATED)
def create_pill_reminder(
    reminder_in: PillReminderCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    new_reminder = PillReminder(
        user_id=current_user.id,
        name=reminder_in.name,
        dosage=reminder_in.dosage,
        time_of_day=reminder_in.time_of_day,
        is_active=reminder_in.is_active
    )
    db.add(new_reminder)
    db.commit()
    db.refresh(new_reminder)
    return new_reminder

@router.put("/{reminder_id}", response_model=PillReminderOut)
def update_pill_reminder(
    reminder_id: int,
    reminder_in: PillReminderUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    reminder = db.query(PillReminder).filter(
        PillReminder.id == reminder_id,
        PillReminder.user_id == current_user.id
    ).first()
    if not reminder:
        raise HTTPException(status_code=404, detail="Pill reminder not found")
    
    update_data = reminder_in.model_dump(exclude_unset=True)
    for key, value in update_data.items():
        setattr(reminder, key, value)
        
    db.commit()
    db.refresh(reminder)
    return reminder

@router.delete("/{reminder_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_pill_reminder(
    reminder_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    reminder = db.query(PillReminder).filter(
        PillReminder.id == reminder_id,
        PillReminder.user_id == current_user.id
    ).first()
    if not reminder:
        raise HTTPException(status_code=404, detail="Pill reminder not found")
        
    db.delete(reminder)
    db.commit()
    return None
