from typing import Optional

import requests

from app.core.config import settings


class LLMClientError(Exception):
    pass


class LLMClient:
    def __init__(
        self,
        base_url: Optional[str] = None,
        model_name: Optional[str] = None,
        timeout_seconds: Optional[int] = None,
    ):
        self.base_url = base_url or settings.LLM_BASE_URL
        self.model_name = model_name or settings.LLM_MODEL_NAME
        self.timeout_seconds = timeout_seconds or settings.LLM_TIMEOUT_SECONDS

    def _ensure_enabled(self) -> None:
        if not settings.LLM_ENABLED:
            raise LLMClientError("LLM отключена в настройках проекта")

    def _truncate_prompt(self, text: str) -> str:
        max_chars = settings.AI_MAX_PROMPT_CHARS
        if len(text) <= max_chars:
            return text
        return text[:max_chars]

    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: Optional[float] = None,
    ) -> str:
        self._ensure_enabled()

        final_prompt = prompt.strip()
        if system_prompt:
            final_prompt = f"{system_prompt.strip()}\n\n{final_prompt}"

        final_prompt = self._truncate_prompt(final_prompt)

        payload = {
            "model": self.model_name,
            "prompt": final_prompt,
            "stream": False,
            "options": {
                "temperature": temperature if temperature is not None else settings.LLM_TEMPERATURE,
            },
        }

        try:
            response = requests.post(
                self.base_url,
                json=payload,
                timeout=self.timeout_seconds,
            )
        except requests.RequestException as exc:
            raise LLMClientError(f"Ошибка подключения к LLM: {exc}") from exc

        if response.status_code != 200:
            raise LLMClientError(
                f"LLM вернула ошибку HTTP {response.status_code}: {response.text}"
            )

        try:
            data = response.json()
        except ValueError as exc:
            raise LLMClientError("LLM вернула некорректный JSON") from exc

        text = data.get("response", "")
        if not text or not text.strip():
            raise LLMClientError("LLM вернула пустой ответ")

        return text.strip()