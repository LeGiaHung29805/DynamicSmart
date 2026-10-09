from __future__ import annotations

import hashlib
import math
import re
import unicodedata
from typing import Protocol

import httpx


class EmbeddingProvider(Protocol):
    name: str

    async def embed(self, texts: list[str]) -> list[list[float]]: ...

    async def embed_query(self, text: str) -> list[float]: ...


class OllamaEmbeddingProvider:
    name = "ollama"

    def __init__(self, base_url: str, model: str, timeout_seconds: float, keep_alive: str):
        self.base_url = base_url
        self.model = model
        self.keep_alive = keep_alive
        timeout = httpx.Timeout(timeout_seconds, connect=min(3.0, timeout_seconds))
        self.client = httpx.AsyncClient(timeout=timeout)

    async def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        response = await self.client.post(
            f"{self.base_url}/api/embed",
            json={"model": self.model, "input": texts, "keep_alive": self.keep_alive},
        )
        response.raise_for_status()
        embeddings = response.json().get("embeddings")
        if not isinstance(embeddings, list) or len(embeddings) != len(texts):
            raise RuntimeError("Ollama trả về embedding không hợp lệ.")
        return [[float(value) for value in vector] for vector in embeddings]

    async def embed_query(self, text: str) -> list[float]:
        instruction = (
            "Instruct: Retrieve Vietnamese ecommerce knowledge that directly answers the query.\n"
            f"Query: {text}"
        )
        return (await self.embed([instruction]))[0]

    async def close(self) -> None:
        await self.client.aclose()


class HashEmbeddingProvider:
    """Deterministic local fallback used only when Ollama is unavailable."""

    name = "hash-fallback"

    def __init__(self, dimensions: int = 384):
        self.dimensions = dimensions

    async def embed(self, texts: list[str]) -> list[list[float]]:
        return [self._embed_one(text) for text in texts]

    async def embed_query(self, text: str) -> list[float]:
        return self._embed_one(text)

    def _embed_one(self, text: str) -> list[float]:
        vector = [0.0] * self.dimensions
        for token in _tokens(text):
            digest = hashlib.blake2b(token.encode("utf-8"), digest_size=8).digest()
            index = int.from_bytes(digest[:4], "big") % self.dimensions
            vector[index] += 1.0
        norm = math.sqrt(sum(value * value for value in vector)) or 1.0
        return [value / norm for value in vector]


def _tokens(value: str) -> list[str]:
    normalized = unicodedata.normalize("NFD", value.lower())
    normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return re.findall(r"[a-z0-9]{2,}", normalized)
