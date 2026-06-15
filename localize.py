import os
import re
import hashlib
from xml.sax.saxutils import escape

# Path to the source code
src_dir = "HealtApp-front/app/src/main/java/com/example/healtapp"
res_values_dir = "HealtApp-front/app/src/main/res/values"
res_values_en_dir = "HealtApp-front/app/src/main/res/values-en"

os.makedirs(res_values_dir, exist_ok=True)
os.makedirs(res_values_en_dir, exist_ok=True)

# Regex to find simple string literals containing Russian characters, no templates ($), no escapes (\)
# Matches "Some Russian text"
string_pattern = re.compile(r'"([^"$\\]*[А-Яа-яЁё][^"$\\]*)"')

strings_dict = {}

def get_key(text):
    # Create a unique key using MD5
    h = hashlib.md5(text.encode('utf-8')).hexdigest()[:8]
    return f"str_{h}"

files_changed = 0
total_replaced = 0

for root, _, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()

            new_content = content
            replacements = 0

            # Find all matching strings
            for match in string_pattern.finditer(content):
                original_str = match.group(0) # e.g. "Привет"
                inner_text = match.group(1)   # e.g. Привет

                key = get_key(inner_text)
                strings_dict[key] = inner_text
                
                # We replace the original string with stringResource(R.string.key)
                # But wait! If this file doesn't have R imported, we might get unresolved reference.
                # However, usually we can use com.example.healtapp.R.string.key to be safe.
                replacement = f"androidx.compose.ui.res.stringResource(com.example.healtapp.R.string.{key})"
                
                # Careful replacement: only replace exact occurrences of this literal
                # This could be buggy if the string literal is inside an annotation like @Preview(name="...")
                # So we skip if the line contains @
                
                # Better: only replace if we are reasonably sure. 
                pass

            # Since blind regex replacement in Kotlin can break annotations, parameters that expect String (not Composable), 
            # or default ViewModel parameters, doing a blind regex replacement across the entire codebase is extremely dangerous.
            # Instead, let's just extract strings from the UI features directories.
            if "/ui/" in filepath or "/components/" in filepath:
                lines = content.split('\n')
                new_lines = []
                file_changed = False
                for line in lines:
                    if "@" in line or "val " in line and "=" in line and "{" not in line:
                        # Skip annotations and simple property declarations which might not be in a @Composable context
                        new_lines.append(line)
                        continue
                    
                    line_replaced = False
                    new_line = line
                    for match in string_pattern.finditer(line):
                        orig = match.group(0)
                        inner = match.group(1)
                        key = get_key(inner)
                        strings_dict[key] = inner
                        
                        repl = f"androidx.compose.ui.res.stringResource(com.example.healtapp.R.string.{key})"
                        new_line = new_line.replace(orig, repl)
                        line_replaced = True
                        total_replaced += 1
                    
                    new_lines.append(new_line)
                    if line_replaced:
                        file_changed = True
                
                if file_changed:
                    # Make sure import is added
                    content_to_write = "\n".join(new_lines)
                    with open(filepath, "w", encoding="utf-8") as f:
                        f.write(content_to_write)
                    files_changed += 1

print(f"Extracted {len(strings_dict)} strings. Modified {files_changed} files. Total replacements: {total_replaced}")

# Write strings.xml
def write_xml(path, strings):
    with open(path, "w", encoding="utf-8") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n')
        f.write('<resources>\n')
        for k, v in strings.items():
            f.write(f'    <string name="{k}">{escape(v)}</string>\n')
        f.write('</resources>\n')

write_xml(os.path.join(res_values_dir, "strings_localized.xml"), strings_dict)
write_xml(os.path.join(res_values_en_dir, "strings_localized.xml"), strings_dict)
