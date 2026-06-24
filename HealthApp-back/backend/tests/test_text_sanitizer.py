from app.llm.text_sanitizer import sanitize_llm_markdown
from app.services.ai.time_context import build_time_context_block, describe_day_period


def test_sanitize_headings_and_bold():
    raw = "### Сон\n\n**Важно:** выпейте воду.\n****"
    cleaned = sanitize_llm_markdown(raw)
    assert "###" not in cleaned
    assert "**" not in cleaned
    assert "Сон" in cleaned
    assert "Важно:" in cleaned


def test_describe_day_period():
    assert describe_day_period(8)[0] == "утро"
    assert describe_day_period(14)[0] == "день"
    assert describe_day_period(19)[0] == "вечер"
    assert describe_day_period(23)[0] == "ночь"


def test_time_context_block_contains_now():
    block = build_time_context_block(sleep_target_hours=8.0)
    assert "ТЕКУЩЕЕ ВРЕМЯ" in block
    assert "Сейчас:" in block
