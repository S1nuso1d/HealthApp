
from app.llm.json_utils import parse_llm_json


def test_parse_llm_json_plain():
    data = parse_llm_json('{"days": [], "grocery_list": []}')
    assert data["days"] == []


def test_parse_llm_json_markdown_fence():
    raw = """Вот план:
```json
{"days": [{"day_name": "Пн"}], "grocery_list": []}
```
"""
    data = parse_llm_json(raw)
    assert data["days"][0]["day_name"] == "Пн"


def test_parse_llm_json_with_prefix_text():
    raw = 'Ответ:\n{"days": [], "grocery_list": [{"name": "овсянка"}]}'
    data = parse_llm_json(raw)
    assert data["grocery_list"][0]["name"] == "овсянка"
