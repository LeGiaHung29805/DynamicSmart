from __future__ import annotations

import httpx


class OllamaChatClient:
    def __init__(self, base_url: str, model: str, timeout_seconds: float):
        self.base_url = base_url
        self.model = model
        self.timeout_seconds = timeout_seconds

    async def answer(self, system_prompt: str, user_prompt: str) -> str:
        timeout = httpx.Timeout(self.timeout_seconds, connect=min(3.0, self.timeout_seconds))
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(
                f"{self.base_url}/api/chat",
                json={
                    "model": self.model,
                    "stream": False,
                    "messages": [
                        {"role": "system", "content": system_prompt},
                        {"role": "user", "content": user_prompt},
                    ],
                    "options": {"temperature": 0.1},
                },
            )
            response.raise_for_status()
            content = response.json().get("message", {}).get("content")
        if not isinstance(content, str) or not content.strip():
            raise RuntimeError("Ollama không trả về nội dung hợp lệ.")
        return content.strip()
