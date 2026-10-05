from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from app.config import Settings
from app.rag.service import RagService
from app.schemas import ChatRequest, ProductInput


class RagServiceTest(unittest.IsolatedAsyncioTestCase):
    def settings(self, root: Path) -> Settings:
        return Settings(
            base_dir=root,
            knowledge_dir=root / "knowledge",
            qdrant_url=None,
            qdrant_api_key=None,
            qdrant_collection="test_knowledge",
            ollama_base_url="http://127.0.0.1:1",
            ollama_chat_model="test",
            ollama_embedding_model="test",
            ollama_timeout_seconds=0.1,
            top_k=3,
            chunk_size=500,
            chunk_overlap=50,
            allow_hash_fallback=True,
        )

    async def test_ingest_falls_back_and_answers_with_citation(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant_dir = root / "knowledge" / "dynamicmart"
            tenant_dir.mkdir(parents=True)
            (tenant_dir / "payment.md").write_text(
                "# Thanh toán\n\nDynamicMart hỗ trợ thanh toán COD khi nhận hàng.",
                encoding="utf-8",
            )
            service = RagService(self.settings(root))

            counts = await service.ingest()
            response = await service.ask(ChatRequest(tenant="dynamicmart", message="Có thanh toán COD không?"))

            self.assertEqual({"dynamicmart": 1}, counts)
            self.assertEqual("hash-fallback", service.embedding_provider.name)
            self.assertEqual("knowledge_question", response.intent)
            self.assertEqual(1, len(response.citations))
            self.assertIn("Thanh toán", response.reply)

    async def test_product_answer_keeps_catalog_values(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            service = RagService(self.settings(Path(directory)))
            response = await service.ask(
                ChatRequest(
                    message="Tìm áo sơ mi",
                    products=[
                        ProductInput(
                            name="Áo sơ mi Oxford",
                            url="https://shop.test/products/oxford",
                            price=550_000,
                            stock_total=7,
                            in_stock=True,
                        )
                    ],
                )
            )

            self.assertEqual("product_search", response.intent)
            self.assertEqual(550_000, response.products[0]["price"])
            self.assertEqual(7, response.products[0]["stock_total"])

    async def test_fallback_extracts_the_sentence_matching_the_question(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant_dir = root / "knowledge" / "fashion-ecommerce"
            tenant_dir.mkdir(parents=True)
            (tenant_dir / "policy.md").write_text(
                "# Chính sách\n\nVoucher được kiểm tra khi checkout. "
                "Thời hạn đổi trả chưa được công bố và cần nhân viên xác nhận.",
                encoding="utf-8",
            )
            service = RagService(self.settings(root))
            await service.ingest()

            response = await service.ask(
                ChatRequest(tenant="fashion-ecommerce", message="Thời hạn đổi trả là bao lâu?")
            )

            self.assertIn("Thời hạn đổi trả", response.reply)

    async def test_sensitive_policy_question_uses_grounded_sentence(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant_dir = root / "knowledge" / "fashion-ecommerce"
            tenant_dir.mkdir(parents=True)
            (tenant_dir / "orders.md").write_text(
                "# Đơn hàng\n\nKhách chỉ có thể tự hủy khi đơn đang ở trạng thái Chờ xử lý. "
                "Đơn đã giao không thể tự hủy.",
                encoding="utf-8",
            )
            service = RagService(self.settings(root))
            await service.ingest()

            async def hallucinated_answer(*_: object) -> str:
                return "Đơn đã giao vẫn có thể tự hủy."

            service.chat_client.answer = hallucinated_answer  # type: ignore[method-assign]
            response = await service.ask(
                ChatRequest(tenant="fashion-ecommerce", message="Khi nào tôi có thể hủy đơn hàng?")
            )

            self.assertIn("Chờ xử lý", response.reply)
            self.assertNotIn("đã giao vẫn có thể", response.reply)

    def test_rejects_numbers_not_present_in_retrieved_context(self) -> None:
        from app.rag.types import Chunk, SearchResult

        results = [
            SearchResult(
                Chunk("fashion-ecommerce", "1", "policy.md", "Đổi trả", "Thời hạn chưa được công bố."),
                0.9,
            )
        ]

        self.assertFalse(RagService._numbers_are_grounded("Có 30 ngày.", results))
        self.assertTrue(RagService._numbers_are_grounded("Thời hạn chưa được công bố.", results))


if __name__ == "__main__":
    unittest.main()
