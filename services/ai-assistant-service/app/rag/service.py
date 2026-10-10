from __future__ import annotations

import asyncio
import re
import unicodedata
from collections import OrderedDict, defaultdict
from collections.abc import AsyncIterator
from typing import Any

import httpx

from app.catalog import DynamicMartCatalogClient
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
            settings.ollama_embedding_keep_alive,
        )
        self.hash_embeddings = HashEmbeddingProvider()
        self.embedding_provider = self.ollama_embeddings
        self.chat_client = OllamaChatClient(
            settings.ollama_base_url,
            settings.ollama_chat_model,
            settings.ollama_timeout_seconds,
            settings.ollama_keep_alive,
            settings.ollama_num_ctx,
            settings.ollama_num_predict,
        )
        self.catalog_client = DynamicMartCatalogClient(
            settings.dynamicmart_catalog_url,
            settings.dynamicmart_storefront_url,
            settings.catalog_timeout_seconds,
        )
        self._ingest_lock = asyncio.Lock()
        self._embedding_cache: OrderedDict[tuple[str, str], list[float]] = OrderedDict()

    async def close(self) -> None:
        self.store.close()
        await self.ollama_embeddings.close()
        await self.chat_client.close()
        await self.catalog_client.close()

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
            self._embedding_cache.clear()
            return self.store.counts_by_tenant()

    async def _embed_groups(self, grouped: dict[str, list[Chunk]], provider: Any) -> dict[str, list[list[float]]]:
        result: dict[str, list[list[float]]] = {}
        for tenant, chunks in grouped.items():
            result[tenant] = await provider.embed([chunk.content for chunk in chunks])
        return result

    async def ask(self, request: ChatRequest) -> ChatResponse:
        contextual_query = self._contextual_query(request)
        product_response = await self._try_product_response(request, contextual_query)
        if product_response is not None:
            return product_response

        relevant = await self._retrieve(request, contextual_query)
        if not relevant:
            return self._not_found_response(request)

        if not self._requires_synthesis(contextual_query):
            reply = self._retrieval_fallback(contextual_query, relevant)
        else:
            try:
                reply = await self.chat_client.answer(
                    self._system_prompt(request.tenant),
                    self._user_prompt(request.message, relevant, request),
                )
                reply = self._normalize_generated_reply(reply)
                if not self._is_grounded_reply(reply, request.message, relevant):
                    reply = self._retrieval_fallback(contextual_query, relevant)
            except Exception:
                reply = self._retrieval_fallback(contextual_query, relevant)

        return self._knowledge_response(request, relevant, reply)

    async def stream(self, request: ChatRequest) -> AsyncIterator[dict[str, Any]]:
        contextual_query = self._contextual_query(request)
        product_response = await self._try_product_response(request, contextual_query)
        if product_response is not None:
            yield {"type": "result", "data": product_response.model_dump()}
            return

        relevant = await self._retrieve(request, contextual_query)
        if not relevant:
            yield {"type": "result", "data": self._not_found_response(request).model_dump()}
            return

        if not self._requires_synthesis(contextual_query):
            response = self._knowledge_response(
                request,
                relevant,
                self._retrieval_fallback(contextual_query, relevant),
            )
            yield {"type": "result", "data": response.model_dump()}
            return

        parts: list[str] = []
        try:
            async for token in self.chat_client.stream_answer(
                self._system_prompt(request.tenant),
                self._user_prompt(request.message, relevant, request),
            ):
                parts.append(token)
                yield {"type": "token", "content": token}
            reply = self._normalize_generated_reply("".join(parts))
            if not self._is_grounded_reply(reply, request.message, relevant):
                reply = self._retrieval_fallback(contextual_query, relevant)
                yield {"type": "replace", "content": reply}
        except Exception:
            reply = self._retrieval_fallback(contextual_query, relevant)
            yield {"type": "replace", "content": reply}
        yield {
            "type": "done",
            "data": self._knowledge_response(request, relevant, reply).model_dump(),
        }

    async def _try_product_response(self, request: ChatRequest, contextual_query: str) -> ChatResponse | None:
        if request.products:
            return self._product_response(request)
        if request.tenant != "dynamicmart":
            return None
        try:
            catalog_products = await self.catalog_client.products_for_question(
                self._catalog_query(request, contextual_query)
            )
        except (httpx.HTTPError, ValueError, TypeError):
            return None
        if catalog_products is None:
            return None
        return self._product_response(request.model_copy(update={"products": catalog_products}))

    async def _retrieve(self, request: ChatRequest, contextual_query: str) -> list[SearchResult]:
        cache_key = (request.tenant, contextual_query.casefold())
        query_embedding = self._embedding_cache.get(cache_key)
        if query_embedding is None:
            embed_query = getattr(self.embedding_provider, "embed_query", None)
            query_embedding = (
                await embed_query(contextual_query)
                if callable(embed_query)
                else (await self.embedding_provider.embed([contextual_query]))[0]
            )
            self._embedding_cache[cache_key] = query_embedding
            self._embedding_cache.move_to_end(cache_key)
            if len(self._embedding_cache) > 128:
                self._embedding_cache.popitem(last=False)
        retrieved = self.store.search(
            request.tenant,
            query_embedding,
            self.settings.top_k,
            query_text=contextual_query,
        )
        return [result for result in retrieved if result.score > 0.02]

    @classmethod
    def _contextual_query(cls, request: ChatRequest) -> str:
        if not request.history or not cls._depends_on_history(request.message):
            return request.message

        safe_turns: list[str] = []
        for turn in request.history[-6:]:
            if turn.role != "user":
                continue
            content = cls._safe_history_content(turn.content)
            if content:
                safe_turns.append(content)
        if not safe_turns:
            return request.message
        context = " ".join(safe_turns)
        return f"{context} {request.message}"[-1800:]

    @classmethod
    def _catalog_query(cls, request: ChatRequest, contextual_query: str) -> str:
        """Resolve short product follow-ups without sending the whole transcript to Catalog."""
        if not request.history or not cls._depends_on_history(request.message):
            return request.message

        for turn in reversed(request.history[-6:]):
            if turn.role != "assistant":
                continue
            safe_content = cls._safe_history_content(turn.content)
            match = re.search(
                r"Sản phẩm đã hiển thị:\s*(.+?)(?:\.|$)",
                safe_content,
                flags=re.IGNORECASE,
            )
            if not match:
                continue
            product_names = [name.strip() for name in match.group(1).split(",") if name.strip()]
            if not product_names:
                continue

            selected_index = 0
            ordinal = re.search(r"(?:mẫu|cái|sản phẩm)?\s*thứ\s*(\d+)", request.message.casefold())
            if ordinal:
                selected_index = max(0, int(ordinal.group(1)) - 1)
            selected_index = min(selected_index, len(product_names) - 1)
            return f"{product_names[selected_index]} {request.message}"

        return contextual_query

    @staticmethod
    def _depends_on_history(message: str) -> bool:
        normalized = message.casefold()
        reference_terms = (
            "nó", "cái đó", "mẫu đó", "mẫu này", "loại đó", "loại này",
            "sản phẩm đó", "sản phẩm này", "cái đầu", "mẫu đầu", "mẫu thứ",
            "thế còn", "vậy còn", "thì sao", "còn cái", "phương thức đó", "trường hợp đó",
            "như trên", "vừa rồi", "ở trên",
        )
        if any(term in normalized for term in reference_terms):
            return True
        words = re.findall(r"[\wÀ-ỹ]+", normalized, flags=re.UNICODE)
        short_follow_up_starts = (
            "còn ", "vậy ", "thế ", "nếu ", "giá ", "phí ",
            "bao nhiêu", "được không", "có không", "khi nào",
        )
        return len(words) <= 9 and normalized.startswith(short_follow_up_starts)

    @staticmethod
    def _safe_history_content(content: str) -> str:
        normalized = " ".join(content.split())
        sensitive_terms = (
            "mật khẩu", "password", "mã otp", " otp ", "access token",
            "refresh token", "cookie", "cvv", "số thẻ", "api key", "secret",
        )
        padded = f" {normalized.casefold()} "
        if any(term in padded for term in sensitive_terms):
            return "[Nội dung nhạy cảm đã được lược bỏ]"
        return normalized[:800]

    def _knowledge_response(
        self,
        request: ChatRequest,
        relevant: list[SearchResult],
        reply: str,
    ) -> ChatResponse:
        return ChatResponse(
            reply=reply,
            intent="knowledge_question",
            query=request.message,
            products=[],
            citations=[self._citation(result) for result in relevant],
        )

    @staticmethod
    def _not_found_response(request: ChatRequest) -> ChatResponse:
        return ChatResponse(
            reply=(
                "Tôi chưa tìm thấy thông tin phù hợp.\n\n"
                "Bạn có thể mô tả cụ thể hơn hoặc liên hệ bộ phận hỗ trợ."
            ),
            intent="knowledge_question",
            query=request.message,
            products=[],
            citations=[],
        )

    def _requires_synthesis(self, question: str) -> bool:
        if not self.settings.enable_llm_synthesis:
            return False
        normalized = question.casefold()
        synthesis_terms = (
            "so sánh", "khác nhau", "tóm tắt", "tổng hợp", "phân tích",
            "tư vấn", "nên chọn", "đề xuất", "giải thích", "vì sao",
        )
        return (
            len(normalized) > 180
            or any(term in normalized for term in synthesis_terms)
        )

    @staticmethod
    def _product_response(request: ChatRequest) -> ChatResponse:
        products = [product.model_dump() for product in request.products]
        in_stock = [product for product in products if product.get("in_stock")]
        count = len(in_stock)
        reply = (
            f"Tôi tìm thấy {count} sản phẩm còn hàng phù hợp.\n\n"
            "Bạn có thể xem thông tin từng sản phẩm bên dưới."
            if count
            else (
                "Tôi chưa tìm thấy sản phẩm đang bán phù hợp với yêu cầu này.\n\n"
                "Bạn có thể thử tên sản phẩm, danh mục hoặc khoảng giá khác."
            )
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
            f"Bạn là trợ lý mua sắm của {tenant}. Chỉ dùng NGỮ CẢNH được cung cấp. "
            "Trả lời tiếng Việt trong tối đa 4 câu, ngắn gọn nhưng đủ ý. "
            "Mỗi ý riêng phải nằm trên một dòng và cách nhau bằng một dòng trống. "
            "Không nhắc tên tài liệu, nguồn tham khảo hoặc cách truy xuất thông tin. "
            "Không tạo thêm bước hướng dẫn, điều kiện, số liệu, chính sách, giá hoặc tồn kho "
            "nếu chúng không xuất hiện nguyên ý trong ngữ cảnh. Nếu thiếu thông tin, nói rõ là chưa có."
        )

    @staticmethod
    def _user_prompt(question: str, results: list[SearchResult], request: ChatRequest) -> str:
        context = "\n\n".join(
            f"--- [TÀI LIỆU {index}: {result.chunk.title}] ---\n{result.chunk.content}"
            for index, result in enumerate(results, start=1)
        )
        history = "\n".join(
            f"{'Khách' if turn.role == 'user' else 'Trợ lý'}: {RagService._safe_history_content(turn.content)}"
            for turn in request.history[-6:]
        )
        history_block = f"LỊCH SỬ GẦN ĐÂY:\n{history}\n\n" if history else ""
        return (
            f"{history_block}NGỮ CẢNH:\n{context}\n\n"
            f"CÂU HỎI: {question}\n\nTRẢ LỜI:"
        )

    @classmethod
    def _retrieval_fallback(cls, question: str, results: list[SearchResult]) -> str:
        candidates: list[tuple[float, float, SearchResult, str]] = []
        question_lower = question.casefold()
        for result in results:
            for sentence in result.chunk.content.replace("\n", ". ").split("."):
                candidate = " ".join(sentence.split()).lstrip("# ")
                if len(candidate) >= 30:
                    title = result.chunk.title.strip()
                    if candidate.casefold().startswith(title.casefold()):
                        candidate = candidate[len(title) :].strip(" :-")
                    if len(candidate) < 30:
                        continue
                    lexical = score_lexical_match(question, candidate)
                    score = (lexical * 0.8) + (result.score * 0.2)
                    candidate_lower = candidate.casefold()
                    is_disclaimer = (
                        "trợ lý không phải nguồn quyết định" in candidate_lower
                        or candidate_lower.startswith("trợ lý không ")
                        or candidate_lower.startswith("chatbot không ")
                    )
                    asks_about_limits = any(term in question_lower for term in ("trợ lý", "chatbot", "giới hạn"))
                    if is_disclaimer and not asks_about_limits:
                        continue
                    candidates.append((score, lexical, result, candidate))

        if not candidates:
            excerpt = " ".join(results[0].chunk.content.split())[:420]
            return excerpt

        candidates.sort(key=lambda item: item[0], reverse=True)
        best_score = candidates[0][0]
        selected: list[tuple[SearchResult, str]] = []
        seen: set[str] = set()
        source_counts: defaultdict[str, int] = defaultdict(int)
        total_length = 0
        for score, lexical, result, candidate in candidates:
            normalized = re.sub(r"\W+", " ", candidate.casefold()).strip()
            if (
                normalized in seen
                or cls._is_redundant_candidate(normalized, seen)
                or source_counts[result.chunk.source] >= 2
            ):
                continue
            if selected and lexical < 0.25:
                continue
            if selected and score < max(0.2, best_score * 0.55):
                break
            if total_length + len(candidate) > 720:
                continue
            selected.append((result, candidate))
            seen.add(normalized)
            source_counts[result.chunk.source] += 1
            total_length += len(candidate)
            if len(selected) >= 3:
                break

        if len(selected) == 1:
            _, excerpt = selected[0]
            return excerpt
        return "\n\n".join(f"• {excerpt}" for _, excerpt in selected)

    @staticmethod
    def _is_redundant_candidate(candidate: str, selected: set[str]) -> bool:
        candidate_tokens = set(candidate.split())
        if not candidate_tokens:
            return True
        for existing in selected:
            existing_tokens = set(existing.split())
            overlap = len(candidate_tokens & existing_tokens)
            denominator = max(1, min(len(candidate_tokens), len(existing_tokens)))
            if overlap / denominator >= 0.55:
                return True
        return False

    @staticmethod
    def _normalize_generated_reply(reply: str) -> str:
        cleaned = re.sub(r"<think>.*?</think>", "", reply, flags=re.DOTALL | re.IGNORECASE)
        cleaned = re.sub(r"\[(?:tài liệu|nguồn)\s*\d+\]", "", cleaned, flags=re.IGNORECASE)
        cleaned = re.sub(
            r"(?i)^\s*(?:theo (?:các )?(?:tài liệu|nguồn)(?: đã kiểm duyệt)?[^:]*:\s*)",
            "",
            cleaned,
        )
        lines = [line.strip() for line in cleaned.replace("\r", "").split("\n") if line.strip()]
        formatted: list[str] = []
        for line in lines:
            if line.startswith(("- ", "• ", "* ")):
                formatted.append(line)
                continue
            sentences = re.split(r"(?<=[.!?])\s+(?=[A-ZÀ-ỸĐ])", line)
            formatted.extend(sentence.strip() for sentence in sentences if sentence.strip())
        return "\n\n".join(formatted).strip()

    @classmethod
    def _is_grounded_reply(cls, reply: str, question: str, results: list[SearchResult]) -> bool:
        if not reply or len(reply.strip()) < 10:
            return False
        if not cls._numbers_are_grounded(reply, results):
            return False
        if any(term in reply.casefold() for term in ("theo tài liệu", "nguồn tham khảo", "tài liệu số")):
            return False
        if not cls._negative_claims_are_grounded(reply, results):
            return False
        # Phát hiện ảo giác trái ngược chính sách nghiêm trọng (hallucination)
        reply_lower = reply.lower()
        context_lower = " ".join(r.chunk.content.lower() for r in results)
        if "không thể tự hủy" in context_lower and ("vẫn có thể tự hủy" in reply_lower or ("có thể tự hủy" in reply_lower and "không thể" not in reply_lower)):
            return False
        return True

    @staticmethod
    def _negative_claims_are_grounded(reply: str, results: list[SearchResult]) -> bool:
        def words(value: str) -> list[str]:
            normalized = unicodedata.normalize("NFD", value.casefold())
            normalized = "".join(char for char in normalized if unicodedata.category(char) != "Mn")
            return re.findall(r"[a-z0-9]{2,}", normalized)

        context = " ".join(words(" ".join(result.chunk.content for result in results)))
        reply_words = words(reply)
        for index, token in enumerate(reply_words):
            if token not in {"khong", "chua"}:
                continue
            phrase = " ".join(reply_words[index:index + 3])
            if len(phrase.split()) >= 3 and phrase not in context:
                return False
        return True

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
        # Bỏ qua các số thứ tự đầu dòng như "1.", "2." hoặc "1)"
        cleaned_reply = re.sub(r"(?m)^\s*\d+[\.\)\-]\s*", "", reply)
        reply_numbers = set(re.findall(r"\d+(?:[.,]\d+)*", cleaned_reply))
        if not reply_numbers:
            return True
        context = " ".join(result.chunk.content for result in results)
        context_numbers = set(re.findall(r"\d+(?:[.,]\d+)*", context))
        # Cho phép các số nhỏ thông dụng từ 1 đến 10
        common_small_numbers = {str(i) for i in range(1, 11)}
        substantive_numbers = {n for n in reply_numbers if n not in common_small_numbers}
        if not substantive_numbers:
            return True
        return substantive_numbers.issubset(context_numbers)
