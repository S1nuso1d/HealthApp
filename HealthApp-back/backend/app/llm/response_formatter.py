from datetime import datetime, timezone

from app.schemas.ai import AIBriefResponse, AIResponse


class ResponseFormatter:
    @staticmethod
    def format_chat_response(text: str, source: str = "llm") -> AIResponse:
        return AIResponse(
            answer=text.strip(),
            generated_at=datetime.now(timezone.utc),
            source=source,
        )

    @staticmethod
    def format_brief_response(
        raw_text: str,
        default_title: str,
        source: str = "llm",
    ) -> AIBriefResponse:
        lines = [line.strip("- ").strip() for line in raw_text.splitlines() if line.strip()]

        title = default_title
        summary = raw_text.strip()
        key_points: list[str] = []

        if lines:
            title = lines[0]

        if len(lines) >= 2:
            summary = lines[1]
            key_points = lines[2:5]
        else:
            summary = raw_text.strip()

        return AIBriefResponse(
            title=title,
            summary=summary,
            key_points=key_points,
            generated_at=datetime.now(timezone.utc),
            source=source,
        )