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
    dynamicmart_catalog_url: str
    dynamicmart_storefront_url: str
    catalog_timeout_seconds: float
    ollama_base_url: str
    ollama_chat_model: str
    ollama_embedding_model: str
    ollama_timeout_seconds: float
    ollama_keep_alive: str
    ollama_num_ctx: int
    ollama_num_predict: int
    enable_llm_synthesis: bool
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

        chunk_size = max(200, int(os.getenv("AI_ASSISTANT_CHUNK_SIZE", "700")))
        chunk_overlap = max(0, int(os.getenv("AI_ASSISTANT_CHUNK_OVERLAP", "100")))
        if chunk_overlap >= chunk_size:
            raise ValueError("AI_ASSISTANT_CHUNK_OVERLAP phải nhỏ hơn AI_ASSISTANT_CHUNK_SIZE.")

        return cls(
            base_dir=base_dir,
            knowledge_dir=knowledge_dir,
            qdrant_url=qdrant_url,
            qdrant_api_key=qdrant_api_key,
            qdrant_collection=os.getenv("QDRANT_COLLECTION", "ecommerce_knowledge_qwen3_06b"),
            dynamicmart_catalog_url=os.getenv(
                "DYNAMICMART_CATALOG_URL",
                "http://127.0.0.1:8082/api/v1/catalog",
            ).rstrip("/"),
            dynamicmart_storefront_url=os.getenv(
                "DYNAMICMART_STOREFRONT_URL",
                "http://localhost:3000",
            ).rstrip("/"),
            catalog_timeout_seconds=max(1.0, float(os.getenv("CATALOG_TIMEOUT_SECONDS", "5"))),
            ollama_base_url=os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/"),
            ollama_chat_model=os.getenv("OLLAMA_CHAT_MODEL", "qwen2.5:0.5b"),
            ollama_embedding_model=os.getenv("OLLAMA_EMBEDDING_MODEL", "qwen3-embedding:0.6b"),
            ollama_timeout_seconds=max(1.0, float(os.getenv("OLLAMA_TIMEOUT_SECONDS", "90"))),
            ollama_keep_alive=os.getenv("OLLAMA_KEEP_ALIVE", "30m"),
            ollama_num_ctx=max(512, int(os.getenv("OLLAMA_NUM_CTX", "2048"))),
            ollama_num_predict=max(32, int(os.getenv("OLLAMA_NUM_PREDICT", "160"))),
            enable_llm_synthesis=_as_bool(os.getenv("RAG_ENABLE_LLM_SYNTHESIS"), False),
            top_k=max(1, min(10, int(os.getenv("AI_ASSISTANT_TOP_K", "5")))),
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            allow_hash_fallback=_as_bool(os.getenv("RAG_ALLOW_HASH_FALLBACK"), True),
        )
