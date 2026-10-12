from __future__ import annotations

import uuid
from dataclasses import dataclass
from typing import Any

from qdrant_client import QdrantClient, models

from app.rag.store import score_lexical_match


@dataclass(frozen=True)
class ProductDocument:
    product_id: str
    text: str
    fingerprint: str


@dataclass(frozen=True)
class ProductHit:
    product_id: str
    score: float


class ProductVectorStore:
    """Semantic product candidates only; live commercial data stays in Catalog Service."""

    TENANT = "dynamicmart"

    def __init__(self, collection_name: str, url: str | None, api_key: str | None):
        self.collection_name = collection_name
        if url:
            self.client = QdrantClient(url=url, api_key=api_key)
            self.mode = "server"
        else:
            self.client = QdrantClient(":memory:")
            self.mode = "memory"

    def close(self) -> None:
        self.client.close()

    def replace(self, documents: list[ProductDocument], embeddings: list[list[float]]) -> None:
        if len(documents) != len(embeddings):
            raise ValueError("Số sản phẩm và embedding không khớp.")
        if not embeddings or not embeddings[0]:
            return
        vector_size = len(embeddings[0])
        if any(len(vector) != vector_size for vector in embeddings):
            raise ValueError("Các product embedding phải có cùng kích thước.")
        self._ensure_collection(vector_size)

        tenant_filter = models.Filter(
            must=[models.FieldCondition(key="tenant", match=models.MatchValue(value=self.TENANT))]
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
                    id=str(uuid.uuid5(uuid.NAMESPACE_URL, f"{self.TENANT}:product:{document.product_id}")),
                    vector=embedding,
                    payload={
                        "tenant": self.TENANT,
                        "product_id": document.product_id,
                        "text": document.text,
                        "fingerprint": document.fingerprint,
                    },
                )
                for document, embedding in zip(documents, embeddings, strict=True)
            ],
            wait=True,
        )

    def search(self, query_embedding: list[float], query_text: str, limit: int = 12) -> list[ProductHit]:
        if not self.client.collection_exists(self.collection_name):
            return []
        tenant_filter = models.Filter(
            must=[models.FieldCondition(key="tenant", match=models.MatchValue(value=self.TENANT))]
        )
        response = self.client.query_points(
            collection_name=self.collection_name,
            query=query_embedding,
            query_filter=tenant_filter,
            limit=limit,
            with_payload=True,
            with_vectors=False,
        )
        hits: list[ProductHit] = []
        for point in response.points:
            payload: dict[str, Any] = point.payload or {}
            product_id = payload.get("product_id")
            if not product_id:
                continue
            lexical = score_lexical_match(query_text, str(payload.get("text") or ""))
            score = (float(point.score) * 0.85) + (lexical * 0.15)
            hits.append(ProductHit(product_id=str(product_id), score=score))
        return sorted(hits, key=lambda hit: hit.score, reverse=True)

    def fingerprint(self) -> str | None:
        if not self.client.collection_exists(self.collection_name):
            return None
        tenant_filter = models.Filter(
            must=[models.FieldCondition(key="tenant", match=models.MatchValue(value=self.TENANT))]
        )
        points, _ = self.client.scroll(
            collection_name=self.collection_name,
            scroll_filter=tenant_filter,
            limit=1,
            with_payload=["fingerprint"],
            with_vectors=False,
        )
        if not points:
            return None
        value = (points[0].payload or {}).get("fingerprint")
        return str(value) if value else None

    def count(self) -> int:
        if not self.client.collection_exists(self.collection_name):
            return 0
        tenant_filter = models.Filter(
            must=[models.FieldCondition(key="tenant", match=models.MatchValue(value=self.TENANT))]
        )
        return int(self.client.count(self.collection_name, count_filter=tenant_filter, exact=True).count)

    def _ensure_collection(self, vector_size: int) -> None:
        if self.client.collection_exists(self.collection_name):
            info = self.client.get_collection(self.collection_name)
            vectors = info.config.params.vectors
            current_size = vectors.size if isinstance(vectors, models.VectorParams) else None
            if current_size == vector_size:
                return
            if self.mode == "server":
                raise ValueError(
                    "Kích thước product vector không khớp; hãy đổi QDRANT_COLLECTION khi đổi embedding model."
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
