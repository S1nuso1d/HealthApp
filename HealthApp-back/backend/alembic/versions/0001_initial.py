"""Bootstrap schema from SQLAlchemy metadata.

Revision ID: 0001_initial
Revises:
Create Date: 2026-09-15
"""

from __future__ import annotations

from typing import Sequence, Union

from alembic import op

# revision identifiers, used by Alembic.
revision: str = "0001_initial"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Первая ревизия создаёт схему из актуальных моделей.
    # Дальнейшие изменения — через `alembic revision --autogenerate`.
    from app.db.database import Base
    import app.models  # noqa: F401

    bind = op.get_bind()
    Base.metadata.create_all(bind=bind)


def downgrade() -> None:
    from app.db.database import Base
    import app.models  # noqa: F401

    bind = op.get_bind()
    Base.metadata.drop_all(bind=bind)
