"""Точечные правки схемы БД без Alembic (колонки к уже существующим таблицам).

`Base.metadata.create_all()` не добавляет новые колонки к старым таблицам — только при первом создании.
"""

from sqlalchemy import inspect, text

from app.db.database import engine


def apply_lightweight_schema_patches() -> None:
    """Добавляет недостающие колонки, которые есть в моделях SQLAlchemy, но отсутствуют в БД."""
    insp = inspect(engine)
    tables = set(insp.get_table_names())

    if "user_profiles" in tables:
        columns = {c["name"] for c in insp.get_columns("user_profiles")}
        if "has_avatar" not in columns:
            dialect = engine.dialect.name
            with engine.begin() as conn:
                if dialect == "postgresql":
                    conn.execute(
                        text(
                            "ALTER TABLE user_profiles ADD COLUMN has_avatar "
                            "BOOLEAN NOT NULL DEFAULT false"
                        )
                    )
                else:
                    conn.execute(
                        text(
                            "ALTER TABLE user_profiles ADD COLUMN has_avatar "
                            "BOOLEAN NOT NULL DEFAULT 0"
                        )
                    )

    if "activity_records" in tables:
        act_cols = {c["name"] for c in insp.get_columns("activity_records")}
        dialect = engine.dialect.name
        with engine.begin() as conn:
            if "avg_power_w" not in act_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE activity_records ADD COLUMN avg_power_w DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE activity_records ADD COLUMN avg_power_w REAL"))
            if "avg_speed_m_s" not in act_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE activity_records ADD COLUMN avg_speed_m_s DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE activity_records ADD COLUMN avg_speed_m_s REAL"))

    if "user_profiles" in tables:
        prof_cols = {c["name"] for c in insp.get_columns("user_profiles")}
        dialect = engine.dialect.name
        with engine.begin() as conn:
            if "target_daily_calories" not in prof_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_daily_calories INTEGER"))
                else:
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_daily_calories INTEGER"))
            if "target_protein_g" not in prof_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_protein_g DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_protein_g REAL"))
            if "target_fat_g" not in prof_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_fat_g DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_fat_g REAL"))
            if "target_carbs_g" not in prof_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_carbs_g DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_carbs_g REAL"))
            if "target_steps" not in prof_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_steps INTEGER"))
                else:
                    conn.execute(text("ALTER TABLE user_profiles ADD COLUMN target_steps INTEGER"))
            if "is_vegetarian" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN is_vegetarian BOOLEAN"))
            if "has_allergies" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN has_allergies BOOLEAN"))
            if "allergies_text" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN allergies_text VARCHAR(500)"))
            if "onboarding_completed" not in prof_cols:
                if dialect == "postgresql":
                    conn.execute(
                        text(
                            "ALTER TABLE user_profiles ADD COLUMN onboarding_completed "
                            "BOOLEAN NOT NULL DEFAULT false"
                        )
                    )
                else:
                    conn.execute(
                        text(
                            "ALTER TABLE user_profiles ADD COLUMN onboarding_completed "
                            "BOOLEAN NOT NULL DEFAULT 0"
                        )
                    )

    if "user_achievements" in tables:
        ach_cols = {c["name"] for c in insp.get_columns("user_achievements")}
        dialect = engine.dialect.name
        with engine.begin() as conn:
            if "achievement_kind" not in ach_cols:
                conn.execute(
                    text(
                        "ALTER TABLE user_achievements ADD COLUMN achievement_kind "
                        "VARCHAR(24) NOT NULL DEFAULT 'daily'"
                    )
                )
            if "progress_current" not in ach_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_achievements ADD COLUMN progress_current DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE user_achievements ADD COLUMN progress_current REAL"))
            if "progress_target" not in ach_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_achievements ADD COLUMN progress_target DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE user_achievements ADD COLUMN progress_target REAL"))
            if "progress_unit" not in ach_cols:
                conn.execute(text("ALTER TABLE user_achievements ADD COLUMN progress_unit VARCHAR(24)"))
            if "record_value" not in ach_cols:
                if dialect == "postgresql":
                    conn.execute(text("ALTER TABLE user_achievements ADD COLUMN record_value DOUBLE PRECISION"))
                else:
                    conn.execute(text("ALTER TABLE user_achievements ADD COLUMN record_value REAL"))
            if "record_label" not in ach_cols:
                conn.execute(text("ALTER TABLE user_achievements ADD COLUMN record_label VARCHAR(128)"))
