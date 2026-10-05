from __future__ import annotations

import re
import unicodedata
import uuid
from collections import Counter
from qdrant_client import QdrantClient, models

from app.rag.types import Chunk, SearchResult


class QdrantVectorStore:
    """Qdrant store supporting embedded, server, and cloud deployments."""

    def __init__(
        self,
        collection_name: str,
        url: str | None = None,
        api_key: str | None = None,
    ):
        self.collection_name = collection_name
        if url:
            self.client = QdrantClient(url=url, api_key=api_key)
            self.mode = "server"
        else:
            self.client = QdrantClient(":memory:")
            self.mode = "memory"

    def close(self) -> None:
        self.client.close()

    def replace_tenant(
        self,
        tenant: str,
        chunks: list[Chunk],
        embeddings: list[list[float]],
        provider: str,
    ) -> None:
        if len(chunks) != len(embeddings):
            raise ValueError("Số chunk và embedding không khớp.")
        if not embeddings or not embeddings[0]:
            raise ValueError("Embedding không được để trống.")

        vector_size = len(embeddings[0])
        if any(len(embedding) != vector_size for embedding in embeddings):
            raise ValueError("Các embedding phải có cùng kích thước.")
        self._ensure_collection(vector_size)

        tenant_filter = models.Filter(
            must=[models.FieldCondition(key="tenant", match=models.MatchValue(value=tenant))]
        )
        self.client.delete(
            collection_name=self.collection_name,
            points_selector=models.FilterSelector(filter=tenant_filter),
            wait=True,
        )
        self.client.upsert(
            collection_name=self.collection_name,
            points=[
                models.PointStruct(
                    id=str(uuid.uuid5(uuid.NAMESPACE_URL, f"{tenant}:{chunk.chunk_id}")),
                    vector=embedding,
                    payload={
                        "tenant": tenant,
                        "chunk_id": chunk.chunk_id,
                        "source": chunk.source,
                        "title": chunk.title,
                        "content": chunk.content,
                        "provider": provider,
                    },
                )
                for chunk, embedding in zip(chunks, embeddings, strict=True)
            ],
            wait=True,
        )

    def search(
        self,
        tenant: str,
        query_embedding: list[float],
        limit: int,
        query_text: str = "",
    ) -> list[SearchResult]:
        if not self.client.collection_exists(self.collection_name):
            return []

        tenant_filter = models.Filter(
            must=[models.FieldCondition(key="tenant", match=models.MatchValue(value=tenant))]
        )
        response = self.client.query_points(
            collection_name=self.collection_name,
            query=query_embedding,
            query_filter=tenant_filter,
            limit=max(limit * 4, limit),
            with_payload=True,
            with_vectors=False,
        )

        results: list[SearchResult] = []
        for point in response.points:
            payload = point.payload or {}
            chunk = Chunk(
                tenant=str(payload.get("tenant", tenant)),
                chunk_id=str(payload.get("chunk_id", point.id)),
                source=str(payload.get("source", "")),
                title=str(payload.get("title", "")),
                content=str(payload.get("content", "")),
            )
            lexical_score = score_lexical_match(query_text, f"{chunk.title} {chunk.content}")
            score = (float(point.score) * 0.7) + (lexical_score * 0.3)
            results.append(SearchResult(chunk=chunk, score=score))
        return sorted(results, key=lambda result: result.score, reverse=True)[:limit]

    def counts_by_tenant(self) -> dict[str, int]:
        if not self.client.collection_exists(self.collection_name):
            return {}

        counts: Counter[str] = Counter()
        offset = None
        while True:
            points, offset = self.client.scroll(
                collection_name=self.collection_name,
                limit=256,
                offset=offset,
                with_payload=["tenant"],
                with_vectors=False,
            )
            for point in points:
                tenant = (point.payload or {}).get("tenant")
                if isinstance(tenant, str):
                    counts[tenant] += 1
            if offset is None:
                break
        return dict(sorted(counts.items()))

    def _ensure_collection(self, vector_size: int) -> None:
        if self.client.collection_exists(self.collection_name):
            info = self.client.get_collection(self.collection_name)
            vectors = info.config.params.vectors
            current_size = vectors.size if isinstance(vectors, models.VectorParams) else None
            if current_size == vector_size:
                return
            if self.mode == "server":
                raise ValueError(
                    "Kích thước vector của Qdrant collection không khớp. "
                    "Hãy dùng QDRANT_COLLECTION mới để re-index an toàn."
                )
            self.client.delete_collection(self.collection_name)

        self.client.create_collection(
            collection_name=self.collection_name,
            vectors_config=models.VectorParams(size=vector_size, distance=models.Distance.COSINE),
        )
        if self.mode == "server":
            self.client.create_payload_index(
                collection_name=self.collection_name,
                field_name="tenant",
                field_schema=models.PayloadSchemaType.KEYWORD,
            )


_STOP_WORDS = {
    "bao", "bi", "cac", "cho", "co", "cua", "duoc", "la", "mot",
    "nhung", "san", "pham", "the", "thi", "toi", "trong", "va",
}


def score_lexical_match(query: str, document: str) -> float:
    query_tokens = [token for token in _tokens(query) if token not in _STOP_WORDS]
    if not query_tokens:
        return 0.0
    unique_query_tokens = set(query_tokens)
    document_tokens = set(_tokens(document))
    matched = sum(1 for token in unique_query_tokens if token in document_tokens)
    score = matched / len(unique_query_tokens)

    normalized_document = " ".join(_tokens(document))
    query_bigrams = {
        " ".join(pair)
        for pair in zip(query_tokens, query_tokens[1:], strict=False)
    }
    if query_bigrams and any(phrase in normalized_document for phrase in query_bigrams):
        score = min(1.0, score + 0.25)
    return score


def _tokens(value: str) -> list[str]:
    normalized = unicodedata.normalize("NFD", value.lower())
    normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
    return re.findall(r"[a-z0-9]{2,}", normalized)
