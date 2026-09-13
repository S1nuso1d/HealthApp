from typing import Literal, Optional, Any

import requests

from app.core.config import settings


class LLMClientError(Exception):
    pass


ChatRole = Literal["system", "user", "assistant"]


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

    def _chat_url(self) -> str:
        url = self.base_url.rstrip("/")
        if settings.LLM_PROVIDER == "openai":
            if not url.endswith("/chat/completions"):
                return url + "/chat/completions"
            return url
        # Ollama logic
        if url.endswith("/api/generate"):
            return url.replace("/api/generate", "/api/chat")
        if url.endswith("/generate"):
            return url.replace("/generate", "/chat")
        if "/api/chat" in url:
            return url
        if "/api/" in url:
            return url.rsplit("/api/", 1)[0] + "/api/chat"
        return url + "/api/chat"

    def _truncate_text(self, text: str, max_chars: int | None = None) -> str:
        limit = max_chars or settings.AI_MAX_PROMPT_CHARS
        if len(text) <= limit:
            return text
        return text[: limit - 80] + "\n\n[…контекст обрезан из-за лимита…]"

    def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: Optional[float] = None,
        json_mode: bool = False,
        ollama_options: Optional[dict[str, Any]] = None,
    ) -> str:
        messages: list[dict[str, str]] = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt.strip()})
        messages.append({"role": "user", "content": prompt.strip()})
        return self.chat(
            messages=messages,
            temperature=temperature,
            json_mode=json_mode,
            ollama_options=ollama_options,
        )

    def analyze_image(
        self,
        base64_image: str,
        prompt: str,
        temperature: Optional[float] = None,
    ) -> str:
        self._ensure_enabled()

        if settings.LLM_PROVIDER != "openai":
            raise LLMClientError("Распознавание по фото поддерживается только для OpenAI")

        payload = {
            "model": self.model_name,
            "messages": [
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/jpeg;base64,{base64_image}"
                            }
                        }
                    ]
                }
            ],
            "temperature": temperature if temperature is not None else settings.LLM_TEMPERATURE,
            "response_format": {"type": "json_object"}
        }
        headers = {
            "Authorization": f"Bearer {settings.LLM_API_KEY}",
            "Content-Type": "application/json",
        }

        try:
            response = requests.post(
                self._chat_url(),
                json=payload,
                headers=headers,
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

        choices = data.get("choices", [])
        if not choices:
            raise LLMClientError("LLM вернула пустой ответ (нет choices)")
        message = choices[0].get("message", {})
        text = message.get("content", "")

        if not text or not str(text).strip():
            raise LLMClientError("LLM вернула пустой ответ")

        return str(text).strip()

    def chat(
        self,
        messages: list[dict[str, str]],
        temperature: Optional[float] = None,
        stream: bool = False,
        json_mode: bool = False,
        ollama_options: Optional[dict[str, Any]] = None,
    ) -> Any:
        self._ensure_enabled()

        trimmed: list[dict[str, str]] = []
        total = 0
        max_chars = settings.AI_MAX_PROMPT_CHARS
        per_msg_limit = 12000 if json_mode else 4000
        for msg in reversed(messages):
            content = self._truncate_text(msg.get("content", "").strip(), max_chars=per_msg_limit)
            if not content:
                continue
            piece = len(content)
            if total + piece > max_chars and trimmed:
                break
            trimmed.insert(0, {"role": msg["role"], "content": content})
            total += piece

        if not trimmed:
            raise LLMClientError("Пустой запрос к LLM")

        if settings.LLM_PROVIDER == "openai":
            payload = {
                "model": self.model_name,
                "messages": trimmed,
                "temperature": temperature if temperature is not None else settings.LLM_TEMPERATURE,
                "stream": stream,
            }
            headers = {
                "Authorization": f"Bearer {settings.LLM_API_KEY}",
                "Content-Type": "application/json",
            }
        else:
            options: dict[str, Any] = {
                "temperature": temperature if temperature is not None else settings.LLM_TEMPERATURE,
            }
            if ollama_options:
                options.update(ollama_options)
            payload = {
                "model": self.model_name,
                "messages": trimmed,
                "stream": stream,
                "options": options,
            }
            if json_mode:
                payload["format"] = "json"
            headers = {"Content-Type": "application/json"}

        if stream:
            try:
                response = requests.post(
                    self._chat_url(),
                    json=payload,
                    headers=headers,
                    timeout=self.timeout_seconds,
                    stream=True,
                )
                response.raise_for_status()

                def generate():
                    import json
                    for line in response.iter_lines():
                        if line:
                            decoded_line = line.decode("utf-8")
                            if settings.LLM_PROVIDER == "openai":
                                if decoded_line.startswith("data: "):
                                    data_str = decoded_line[6:]
                                    if data_str == "[DONE]":
                                        break
                                    try:
                                        data = json.loads(data_str)
                                        choices = data.get("choices", [])
                                        if choices:
                                            delta = choices[0].get("delta", {})
                                            content = delta.get("content", "")
                                            if content:
                                                yield content
                                    except ValueError:
                                        pass
                            else:
                                try:
                                    data = json.loads(decoded_line)
                                    message = data.get("message", {})
                                    content = message.get("content", "")
                                    if content:
                                        yield content
                                except ValueError:
                                    pass
                return generate()
            except requests.RequestException as exc:
                raise LLMClientError(f"Ошибка подключения к LLM: {exc}") from exc

        try:
            response = requests.post(
                self._chat_url(),
                json=payload,
                headers=headers,
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

        if settings.LLM_PROVIDER == "openai":
            choices = data.get("choices", [])
            if not choices:
                raise LLMClientError("LLM вернула пустой ответ (нет choices)")
            message = choices[0].get("message", {})
            text = message.get("content", "")
        else:
            message = data.get("message") or {}
            text = message.get("content") or data.get("response", "")
            
        if not text or not str(text).strip():
            raise LLMClientError("LLM вернула пустой ответ")

        return str(text).strip()

    def check_availability(self) -> tuple[bool, str]:
        if not settings.LLM_ENABLED:
            return False, "LLM отключена (LLM_ENABLED=false)"
        if settings.LLM_PROVIDER == "openai":
            if not settings.LLM_API_KEY:
                return False, "Не задан LLM_API_KEY для OpenAI"
            return True, f"OpenAI настроен, модель {self.model_name}"
        base = self.base_url.rstrip("/")
        if "/api/" in base:
            base = base.rsplit("/api/", 1)[0]
        tags_url = f"{base}/api/tags"
        try:
            response = requests.get(tags_url, timeout=min(8, self.timeout_seconds))
        except requests.RequestException as exc:
            return False, (
                f"Ollama недоступна по {tags_url}: {exc}. "
                "На машине с backend выполните: ollama serve && ollama pull "
                f"{self.model_name}"
            )
        if response.status_code != 200:
            return False, f"Ollama HTTP {response.status_code} ({tags_url})"
        try:
            models = [m.get("name", "") for m in response.json().get("models", [])]
        except ValueError:
            return False, "Ollama вернула некорректный JSON"
        if self._ollama_model_installed(models):
            return True, f"Ollama доступна, модель {self.model_name} найдена"
        preview = ", ".join(models[:4]) if models else "список пуст"
        if models:
            return True, (
                f"Ollama доступна. В настройках указана {self.model_name}, "
                f"установлены модели: {preview}"
            )
        return False, (
            f"Ollama запущена, но модели не найдены. Выполните: ollama pull {self.model_name}"
        )

    def _ollama_model_installed(self, models: list[str]) -> bool:
        wanted = (self.model_name or "").strip().lower()
        if not wanted:
            return bool(models)
        wanted_base = wanted.split(":", 1)[0]
        for raw in models:
            name = (raw or "").strip().lower()
            if not name:
                continue
            if name == wanted or name.startswith(f"{wanted}:"):
                return True
            base = name.split(":", 1)[0]
            if base == wanted_base:
                return True
        return False
