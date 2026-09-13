from sqlalchemy import create_engine, MetaData, text

def migrate(sqlite_url: str, pg_url: str):
    print(f"Connecting to SQLite: {sqlite_url}")
    sqlite_engine = create_engine(sqlite_url)

    print(f"Connecting to PostgreSQL: {pg_url}")
    try:
        pg_engine = create_engine(pg_url)
        # Test connection
        with pg_engine.connect() as conn:
            conn.execute(text("SELECT 1"))
    except Exception as e:
        print(f"Failed to connect to PostgreSQL: {e}")
        return

    # Отбиваем структуру из SQLite
    metadata = MetaData()
    metadata.reflect(bind=sqlite_engine)
    
    # Создаем таблицы в PostgreSQL
    print("Creating tables in PostgreSQL...")
    metadata.create_all(bind=pg_engine)

    # Копируем данные
    for table_name in metadata.tables:
        table = metadata.tables[table_name]
        print(f"Migrating table: {table_name}...")
        
        with sqlite_engine.connect() as sqlite_conn:
            records = sqlite_conn.execute(table.select()).fetchall()
            
        if not records:
            print(f"  Table {table_name} is empty, skipping.")
            continue
        
        # Получаем ключи (названия колонок) из первого кортежа
        keys = records[0]._mapping.keys()
        data_to_insert = [dict(zip(keys, row, strict=True)) for row in records]
        
        with pg_engine.connect() as pg_conn:
            # Очищаем таблицу в Postgres перед вставкой
            pg_conn.execute(table.delete())
            
            # Вставляем данные
            try:
                pg_conn.execute(table.insert(), data_to_insert)
                pg_conn.commit()
                print(f"  Successfully migrated {len(records)} rows for {table_name}.")
            except Exception as e:
                print(f"  Error migrating table {table_name}: {e}")
                pg_conn.rollback()

    # Сбрасываем счетчики SEQUENCE в PostgreSQL для таблиц с автоинкрементом
    print("Resetting sequences in PostgreSQL...")
    with pg_engine.connect() as pg_conn:
        for table_name in metadata.tables:
            try:
                # В PostgreSQL последовательности обычно называются table_id_seq
                # Безопасный способ - использовать setval с max(id)
                pg_conn.execute(text(f"SELECT setval(pg_get_serial_sequence('{table_name}', 'id'), coalesce(max(id), 1), max(id) IS NOT null) FROM {table_name};"))
                pg_conn.commit()
            except Exception:
                pg_conn.rollback()
                # Это нормально, если у таблицы нет колонки id
                pass
            
    print("Migration completed successfully!")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(description="Migrate SQLite to PostgreSQL")
    parser.add_argument("--sqlite", default="sqlite:///./healthapp.db", help="SQLite connection string")
    parser.add_argument("--pg", required=True, help="PostgreSQL connection string (e.g. postgresql://postgres:password@localhost:5432/health_app_db)")
    
    args = parser.parse_args()
    migrate(args.sqlite, args.pg)
