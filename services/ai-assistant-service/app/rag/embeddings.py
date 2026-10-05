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


class OllamaEmbeddingProvider:
    name = "ollama"

    def __init__(self, base_url: str, model: str, timeout_seconds: float):
        self.base_url = base_url
        self.model = model
        self.timeout_seconds = timeout_seconds

    async def embed(self, texts: list[str]) -> list[list[float]]:
        if not texts:
            return []
        timeout = httpx.Timeout(self.timeout_seconds, connect=min(3.0, self.timeout_seconds))
        async with httpx.AsyncClient(timeout=timeout) as client:
            response = await client.post(
                f"{self.base_url}/api/embed",
                json={"model": self.model, "input": texts},
            )
            response.raise_for_status()
            embeddings = response.json().get("embeddings")
        if not isinstance(embeddings, list) or len(embeddings) != len(texts):
            raise RuntimeError("Ollama trả về embedding không hợp lệ.")
        return [[float(value) for value in vector] for vector in embeddings]


class HashEmbeddingProvider:
    """Deterministic local fallback used only when Ollama is unavailable."""

    name = "hash-fallback"

    def __init__(self, dimensions: int = 384):
        self.dimensions = dimensions

    async def embed(self, texts: list[str]) -> list[list[float]]:
        return [self._embed_one(text) for text in texts]

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
