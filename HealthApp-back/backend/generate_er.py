import sys
from app.db.database import Base
from app.models import *  # This triggers registry
from sqlalchemy.orm import class_mapper

def generate_mermaid_er():
    print("```mermaid")
    print("erDiagram")
    
    seen_relationships = set()
    
    for mapper in Base.registry.mappers:
        table = mapper.local_table
        table_name = table.name
        
        # Columns
        print(f"    {table_name} {{")
        for column in table.columns:
            pk = "PK" if column.primary_key else ""
            fk = "FK" if column.foreign_keys else ""
            key_info = f"{pk},{fk}".strip(",").strip()
            key_info_str = f" {key_info}" if key_info else ""
            type_name = str(column.type).replace(" ", "_").replace("(", "[").replace(")", "]")
            print(f"        {type_name} {column.name}{key_info_str}")
        print("    }")
        
        # Relationships
        for rel in mapper.relationships:
            if rel.direction.name in ('MANYTOONE', 'ONETOMANY'):
                target_table = rel.target.name
                
                if rel.direction.name == 'MANYTOONE':
                    rel_tuple = (target_table, table_name)
                    if rel_tuple not in seen_relationships:
                        print(f"    {target_table} ||--o{{ {table_name} : \"{rel.key}\"")
                        seen_relationships.add(rel_tuple)
    print("```")

if __name__ == "__main__":
    generate_mermaid_er()
