from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path


def _as_bool(value: str | None, default: bool) -> bool:
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Settings:
    base_dir: Path
    knowledge_dir: Path
    qdrant_url: str | None
    qdrant_api_key: str | None
    qdrant_collection: str
    ollama_base_url: str
    ollama_chat_model: str
    ollama_embedding_model: str
    ollama_timeout_seconds: float
    top_k: int
    chunk_size: int
    chunk_overlap: int
    allow_hash_fallback: bool

    @classmethod
    def from_environment(cls) -> "Settings":
        base_dir = Path(__file__).resolve().parents[1]
        knowledge_dir = Path(os.getenv("AI_ASSISTANT_KNOWLEDGE_DIR", "knowledge"))
        if not knowledge_dir.is_absolute():
            knowledge_dir = base_dir / knowledge_dir

        qdrant_url = os.getenv("QDRANT_URL", "http://127.0.0.1:6333").strip() or None
        qdrant_api_key = os.getenv("QDRANT_API_KEY", "").strip() or None

        chunk_size = max(200, int(os.getenv("AI_ASSISTANT_CHUNK_SIZE", "900")))
        chunk_overlap = max(0, int(os.getenv("AI_ASSISTANT_CHUNK_OVERLAP", "150")))
        if chunk_overlap >= chunk_size:
            raise ValueError("AI_ASSISTANT_CHUNK_OVERLAP phải nhỏ hơn AI_ASSISTANT_CHUNK_SIZE.")

        return cls(
            base_dir=base_dir,
            knowledge_dir=knowledge_dir,
            qdrant_url=qdrant_url,
            qdrant_api_key=qdrant_api_key,
            qdrant_collection=os.getenv("QDRANT_COLLECTION", "ecommerce_knowledge"),
            ollama_base_url=os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/"),
            ollama_chat_model=os.getenv("OLLAMA_CHAT_MODEL", "qwen2.5:0.5b"),
            ollama_embedding_model=os.getenv("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text"),
            ollama_timeout_seconds=max(1.0, float(os.getenv("OLLAMA_TIMEOUT_SECONDS", "45"))),
            top_k=max(1, min(10, int(os.getenv("AI_ASSISTANT_TOP_K", "4")))),
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            allow_hash_fallback=_as_bool(os.getenv("RAG_ALLOW_HASH_FALLBACK"), True),
        )
