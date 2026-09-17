"""Точечные правки схемы БД без Alembic (колонки к уже существующим таблицам).

`Base.metadata.create_all()` не добавляет новые колонки к старым таблицам — только при первом создании.
"""

from sqlalchemy import inspect, text

from app.db.database import engine, Base


def apply_lightweight_schema_patches() -> None:
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
            if "first_name" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN first_name VARCHAR(64)"))
            if "last_name" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN last_name VARCHAR(64)"))
            if "nickname" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN nickname VARCHAR(32)"))
            if "current_streak" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN current_streak INTEGER NOT NULL DEFAULT 0"))
            if "last_active_date" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN last_active_date VARCHAR(10)"))
            if "birth_date" not in prof_cols:
                conn.execute(text("ALTER TABLE user_profiles ADD COLUMN birth_date VARCHAR(10)"))

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

    if "challenges" not in tables:
        Base.metadata.create_all(bind=engine, tables=[Base.metadata.tables["challenges"], Base.metadata.tables["challenge_participants"]])

    if "feed_story_views" not in tables:
        Base.metadata.create_all(bind=engine, tables=[Base.metadata.tables["feed_story_views"]])

    if "revoked_refresh_tokens" not in tables:
        Base.metadata.create_all(
            bind=engine, tables=[Base.metadata.tables["revoked_refresh_tokens"]]
        )

    if "habit_experiments" not in tables and "habit_experiments" in Base.metadata.tables:
        Base.metadata.create_all(
            bind=engine, tables=[Base.metadata.tables["habit_experiments"]]
        )

    if "users" in tables:
        user_cols = {c["name"] for c in insp.get_columns("users")}
        if "tokens_valid_from" not in user_cols:
            with engine.begin() as conn:
                conn.execute(
                    text("ALTER TABLE users ADD COLUMN tokens_valid_from TIMESTAMP")
                )

    _create_missing_indexes(insp, tables)


# Составные индексы «пользователь + дата»: почти каждый запрос дашборда и аналитики
# фильтрует именно так, а одиночных индексов для этого недостаточно.
_COMPOSITE_INDEXES: tuple[tuple[str, str, tuple[str, ...]], ...] = (
    ("sleep_records", "ix_sleep_records_user_sleep_end", ("user_id", "sleep_end")),
    ("sleep_records", "ix_sleep_records_user_sleep_start", ("user_id", "sleep_start")),
    ("meal_records", "ix_meal_records_user_meal_time", ("user_id", "meal_time")),
    (
        "hydration_records",
        "ix_hydration_records_user_record_time",
        ("user_id", "record_time"),
    ),
    (
        "activity_records",
        "ix_activity_records_user_start_time",
        ("user_id", "start_time"),
    ),
    (
        "daily_health_summaries",
        "ix_daily_health_summaries_user_date",
        ("user_id", "summary_date"),
    ),
)


def _create_missing_indexes(insp, tables: set[str]) -> None:
    """`create_all` не добавляет индексы к уже существующим таблицам — делаем вручную."""
    for table, index_name, columns in _COMPOSITE_INDEXES:
        if table not in tables:
            continue
        existing = {idx["name"] for idx in insp.get_indexes(table)}
        if index_name in existing:
            continue
        column_list = ", ".join(columns)
        with engine.begin() as conn:
            conn.execute(
                text(
                    f"CREATE INDEX IF NOT EXISTS {index_name} "
                    f"ON {table} ({column_list})"
                )
            )

