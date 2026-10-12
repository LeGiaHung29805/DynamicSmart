from __future__ import annotations

import json
from collections.abc import AsyncIterator

import httpx


class OllamaChatClient:
    def __init__(
        self,
        base_url: str,
        model: str,
        timeout_seconds: float,
        keep_alive: str,
        num_ctx: int,
        num_predict: int,
    ):
        self.base_url = base_url
        self.model = model
        self.keep_alive = keep_alive
        self.options = {
            "temperature": 0.1,
            "num_ctx": num_ctx,
            "num_predict": num_predict,
        }
        timeout = httpx.Timeout(timeout_seconds, connect=min(3.0, timeout_seconds))
        self.client = httpx.AsyncClient(timeout=timeout)

    def _payload(self, system_prompt: str, user_prompt: str, *, stream: bool) -> dict[str, object]:
        return {
            "model": self.model,
            "stream": stream,
            "think": False,
            "keep_alive": self.keep_alive,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            "options": self.options,
        }

    async def answer(self, system_prompt: str, user_prompt: str) -> str:
        response = await self.client.post(
            f"{self.base_url}/api/chat",
            json=self._payload(system_prompt, user_prompt, stream=False),
        )
        response.raise_for_status()
        content = response.json().get("message", {}).get("content")
        if not isinstance(content, str) or not content.strip():
            raise RuntimeError("Ollama không trả về nội dung hợp lệ.")
        return content.strip()

    async def stream_answer(self, system_prompt: str, user_prompt: str) -> AsyncIterator[str]:
        async with self.client.stream(
            "POST",
            f"{self.base_url}/api/chat",
            json=self._payload(system_prompt, user_prompt, stream=True),
        ) as response:
            response.raise_for_status()
            async for line in response.aiter_lines():
                if not line:
                    continue
                payload = json.loads(line)
                content = payload.get("message", {}).get("content")
                if isinstance(content, str) and content:
                    yield content

    async def close(self) -> None:
        await self.client.aclose()
