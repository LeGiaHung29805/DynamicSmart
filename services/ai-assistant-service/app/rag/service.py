from __future__ import annotations

import asyncio
import re
from collections import defaultdict
from typing import Any

from app.config import Settings
from app.rag.embeddings import HashEmbeddingProvider, OllamaEmbeddingProvider
from app.rag.loader import DirectoryDocumentLoader
from app.rag.ollama import OllamaChatClient
from app.rag.splitter import RecursiveTextSplitter
from app.rag.store import QdrantVectorStore, score_lexical_match
from app.rag.types import Chunk, SearchResult
from app.schemas import ChatRequest, ChatResponse, Citation


class RagService:
    def __init__(self, settings: Settings):
        self.settings = settings
        self.store = QdrantVectorStore(
            collection_name=settings.qdrant_collection,
            url=settings.qdrant_url,
            api_key=settings.qdrant_api_key,
        )
        self.ollama_embeddings = OllamaEmbeddingProvider(
            settings.ollama_base_url,
            settings.ollama_embedding_model,
            settings.ollama_timeout_seconds,
        )
        self.hash_embeddings = HashEmbeddingProvider()
        self.embedding_provider = self.ollama_embeddings
        self.chat_client = OllamaChatClient(
            settings.ollama_base_url,
            settings.ollama_chat_model,
            settings.ollama_timeout_seconds,
        )
        self._ingest_lock = asyncio.Lock()

    def close(self) -> None:
        self.store.close()

    async def ingest(self) -> dict[str, int]:
        async with self._ingest_lock:
            documents = DirectoryDocumentLoader(self.settings.knowledge_dir).load()
            chunks = RecursiveTextSplitter(
                self.settings.chunk_size,
                self.settings.chunk_overlap,
            ).split_documents(documents)
            grouped: dict[str, list[Chunk]] = defaultdict(list)
            for chunk in chunks:
                grouped[chunk.tenant].append(chunk)

            provider = self.ollama_embeddings
            try:
                embedded = await self._embed_groups(grouped, provider)
            except Exception:
                if not self.settings.allow_hash_fallback:
                    raise
                provider = self.hash_embeddings
                embedded = await self._embed_groups(grouped, provider)

            for tenant, tenant_chunks in grouped.items():
                self.store.replace_tenant(tenant, tenant_chunks, embedded[tenant], provider.name)
            self.embedding_provider = provider
            return self.store.counts_by_tenant()

    async def _embed_groups(self, grouped: dict[str, list[Chunk]], provider: Any) -> dict[str, list[list[float]]]:
        result: dict[str, list[list[float]]] = {}
        for tenant, chunks in grouped.items():
            result[tenant] = await provider.embed([chunk.content for chunk in chunks])
        return result

    async def ask(self, request: ChatRequest) -> ChatResponse:
        if request.products:
            return self._product_response(request)

        query_embedding = (await self.embedding_provider.embed([request.message]))[0]
        retrieved = self.store.search(
            request.tenant,
            query_embedding,
            self.settings.top_k,
            query_text=request.message,
        )
        relevant = [result for result in retrieved if result.score > 0.02]
        if not relevant:
            return ChatResponse(
                reply=(
                    "Tôi chưa tìm thấy thông tin phù hợp trong tài liệu đã được kiểm duyệt. "
                    "Bạn có thể hỏi rõ hơn hoặc liên hệ bộ phận hỗ trợ."
                ),
                intent="knowledge_question",
                query=request.message,
                products=[],
                citations=[],
            )

        if self._requires_extractive_answer(request.message):
            reply = self._retrieval_fallback(request.message, relevant)
        else:
            try:
                reply = await self.chat_client.answer(
                    self._system_prompt(request.tenant),
                    self._user_prompt(request.message, relevant),
                )
                if not self._numbers_are_grounded(reply, relevant):
                    reply = self._retrieval_fallback(request.message, relevant)
            except Exception:
                reply = self._retrieval_fallback(request.message, relevant)

        return ChatResponse(
            reply=reply,
            intent="knowledge_question",
            query=request.message,
            products=[],
            citations=[self._citation(result) for result in relevant],
        )

    @staticmethod
    def _product_response(request: ChatRequest) -> ChatResponse:
        products = [product.model_dump() for product in request.products]
        in_stock = [product for product in products if product.get("in_stock")]
        count = len(in_stock)
        reply = (
            f"Tôi tìm thấy {count} sản phẩm còn hàng phù hợp. "
            "Giá và tồn kho bên dưới được lấy trực tiếp từ danh mục của cửa hàng."
            if count
            else "Tôi tìm thấy sản phẩm phù hợp nhưng hiện chưa có mẫu nào còn hàng."
        )
        return ChatResponse(
            reply=reply,
            intent="product_search",
            query=request.message,
            products=products,
            citations=[],
        )

    @staticmethod
    def _system_prompt(tenant: str) -> str:
        return (
            f"Bạn là trợ lý mua sắm của {tenant}. Chỉ trả lời bằng tiếng Việt dựa trên NGỮ CẢNH. "
            "Không bịa giá, tồn kho, chính sách, trạng thái đơn hoặc dữ liệu cá nhân. "
            "Nếu ngữ cảnh không đủ, nói rõ chưa có thông tin. Trả lời ngắn gọn, dễ hành động."
        )

    @staticmethod
    def _user_prompt(question: str, results: list[SearchResult]) -> str:
        context = "\n\n".join(
            f"[Nguồn {index}: {result.chunk.title}]\n{result.chunk.content}"
            for index, result in enumerate(results, start=1)
        )
        return f"NGỮ CẢNH:\n{context}\n\nCÂU HỎI: {question}\n\nCÂU TRẢ LỜI:"

    @staticmethod
    def _retrieval_fallback(question: str, results: list[SearchResult]) -> str:
        sentences: list[tuple[SearchResult, str]] = []
        for result in results:
            for sentence in result.chunk.content.replace("\n", ". ").split("."):
                candidate = " ".join(sentence.split()).lstrip("# ")
                if len(candidate) >= 30:
                    sentences.append((result, candidate))
        best_result, excerpt = max(
            sentences,
            key=lambda item: score_lexical_match(question, item[1]) + (item[0].score * 0.05),
            default=(results[0], results[0].chunk.content),
        )
        if len(excerpt) > 420:
            excerpt = excerpt[:417].rstrip() + "..."
        return f"Theo tài liệu “{best_result.chunk.title}”: {excerpt}"

    @staticmethod
    def _requires_extractive_answer(question: str) -> bool:
        normalized = question.casefold()
        sensitive_terms = (
            "đơn hàng",
            "trạng thái đơn",
            "hủy",
            "huỷ",
            "thanh toán",
            "hoàn tiền",
            "đổi trả",
            "voucher",
            "mã giảm",
            "giá",
            "tồn kho",
            "vận chuyển",
            "giao hàng",
            "mật khẩu",
            "otp",
        )
        return any(term in normalized for term in sensitive_terms)

    @staticmethod
    def _citation(result: SearchResult) -> Citation:
        excerpt = " ".join(result.chunk.content.split())
        if len(excerpt) > 180:
            excerpt = excerpt[:177].rstrip() + "..."
        return Citation(
            source=result.chunk.source,
            title=result.chunk.title,
            excerpt=excerpt,
            score=round(result.score, 6),
        )

    @staticmethod
    def _numbers_are_grounded(reply: str, results: list[SearchResult]) -> bool:
        reply_numbers = set(re.findall(r"\d+(?:[.,]\d+)*", reply))
        if not reply_numbers:
            return True
        context = " ".join(result.chunk.content for result in results)
        context_numbers = set(re.findall(r"\d+(?:[.,]\d+)*", context))
        return reply_numbers.issubset(context_numbers)
