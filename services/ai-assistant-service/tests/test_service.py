from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from app.config import Settings
from app.rag.service import RagService
from app.schemas import ChatRequest, ChatTurn, ProductInput


class RagServiceTest(unittest.IsolatedAsyncioTestCase):
    def settings(self, root: Path) -> Settings:
        return Settings(
            base_dir=root,
            knowledge_dir=root / "knowledge",
            qdrant_url=None,
            qdrant_api_key=None,
            qdrant_collection="test_knowledge",
            dynamicmart_catalog_url="http://127.0.0.1:1/api/v1/catalog",
            dynamicmart_storefront_url="http://localhost:3000",
            catalog_timeout_seconds=0.1,
            ollama_base_url="http://127.0.0.1:1",
            ollama_chat_model="test",
            ollama_embedding_model="test",
            ollama_timeout_seconds=0.1,
            ollama_keep_alive="30m",
            ollama_embedding_keep_alive="30m",
            ollama_num_ctx=2048,
            ollama_num_predict=160,
            enable_llm_synthesis=True,
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
            self.assertIn("COD", response.reply)
            self.assertNotIn("Theo tài liệu", response.reply)

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

    async def test_dynamicmart_product_question_uses_live_catalog_products(self) -> None:
        class FakeCatalogClient:
            async def products_for_question(self, _: str) -> list[ProductInput]:
                return [
                    ProductInput(
                        id="product-1",
                        slug="dynamic-phone-pro",
                        name="Dynamic Phone Pro",
                        url="http://localhost:3000/products/dynamic-phone-pro",
                        price=15_490_000,
                        compare_at_price=15_990_000,
                        in_stock=True,
                    )
                ]

        with tempfile.TemporaryDirectory() as directory:
            service = RagService(self.settings(Path(directory)))
            service.catalog_client = FakeCatalogClient()  # type: ignore[assignment]

            response = await service.ask(
                ChatRequest(tenant="dynamicmart", message="Tìm điện thoại dưới 20 triệu")
            )

            self.assertEqual("product_search", response.intent)
            self.assertEqual("Dynamic Phone Pro", response.products[0]["name"])
            self.assertEqual(15_490_000, response.products[0]["price"])

    async def test_product_follow_up_resolves_product_name_from_history(self) -> None:
        class FakeCatalogClient:
            query = ""

            async def products_for_question(self, query: str) -> list[ProductInput]:
                self.query = query
                return [
                    ProductInput(
                        name="Dynamic Phone Pro",
                        url="http://localhost:3000/products/dynamic-phone-pro",
                        price=15_490_000,
                        stock_total=3,
                        in_stock=True,
                    )
                ]

        with tempfile.TemporaryDirectory() as directory:
            service = RagService(self.settings(Path(directory)))
            client = FakeCatalogClient()
            service.catalog_client = client  # type: ignore[assignment]

            response = await service.ask(
                ChatRequest(
                    tenant="dynamicmart",
                    message="Mẫu đó còn hàng không?",
                    history=[
                        ChatTurn(role="user", content="Tìm điện thoại dưới 20 triệu"),
                        ChatTurn(
                            role="assistant",
                            content="Tôi tìm thấy sản phẩm. Sản phẩm đã hiển thị: Dynamic Phone Pro.",
                        ),
                    ],
                )
            )

            self.assertIn("Dynamic Phone Pro", client.query)
            self.assertEqual("Dynamic Phone Pro", response.products[0]["name"])

    async def test_follow_up_knowledge_question_uses_history_for_retrieval(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant_dir = root / "knowledge" / "dynamicmart"
            tenant_dir.mkdir(parents=True)
            (tenant_dir / "shipping.md").write_text(
                "# Vận chuyển\n\nPhí vận chuyển thay đổi theo địa chỉ nhận hàng và được hiển thị trước khi xác nhận.",
                encoding="utf-8",
            )
            service = RagService(self.settings(root))
            await service.ingest()

            response = await service.ask(
                ChatRequest(
                    tenant="dynamicmart",
                    message="Nếu đổi địa chỉ thì sao?",
                    history=[
                        ChatTurn(role="user", content="Phí vận chuyển được tính thế nào?"),
                        ChatTurn(role="assistant", content="Phí được hiển thị trước khi xác nhận."),
                    ],
                )
            )

            self.assertIn("địa chỉ nhận hàng", response.reply)

    def test_sensitive_history_is_redacted_from_context(self) -> None:
        request = ChatRequest(
            message="Cái đó thì sao?",
            history=[ChatTurn(role="user", content="Mật khẩu của tôi là 123456")],
        )

        contextual_query = RagService._contextual_query(request)

        self.assertIn("[Nội dung nhạy cảm đã được lược bỏ]", contextual_query)
        self.assertNotIn("123456", contextual_query)

    def test_short_follow_up_is_resolved_from_history(self) -> None:
        request = ChatRequest(
            message="Còn phí giao hàng?",
            history=[ChatTurn(role="user", content="Tôi muốn đổi địa chỉ nhận hàng")],
        )

        self.assertEqual(
            "Tôi muốn đổi địa chỉ nhận hàng Còn phí giao hàng?",
            RagService._contextual_query(request),
        )

    def test_generated_reply_is_cleaned_and_keeps_readable_spacing(self) -> None:
        reply = RagService._normalize_generated_reply(
            "<think>internal</think>\nTheo tài liệu đã kiểm duyệt: Ý thứ nhất.\n- Ý thứ hai."
        )

        self.assertEqual("Ý thứ nhất.\n\n- Ý thứ hai.", reply)

        prose = RagService._normalize_generated_reply("Ý thứ nhất. Ý thứ hai rõ ràng hơn.")
        self.assertEqual("Ý thứ nhất.\n\nÝ thứ hai rõ ràng hơn.", prose)

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

    async def test_simple_faq_does_not_wait_for_chat_model(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant_dir = root / "knowledge" / "dynamicmart"
            tenant_dir.mkdir(parents=True)
            (tenant_dir / "shipping.md").write_text(
                "# Vận chuyển\n\nPhí vận chuyển được hiển thị trước khi khách xác nhận đơn hàng.",
                encoding="utf-8",
            )
            service = RagService(self.settings(root))
            await service.ingest()

            async def unexpected_answer(*_: object) -> str:
                self.fail("FAQ đơn giản không được gọi chat model.")

            service.chat_client.answer = unexpected_answer  # type: ignore[method-assign]
            response = await service.ask(
                ChatRequest(tenant="dynamicmart", message="Phí vận chuyển tính thế nào?")
            )

            self.assertIn("Phí vận chuyển", response.reply)
            self.assertEqual(1, len(response.citations))

    async def test_complex_question_uses_chat_model(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant_dir = root / "knowledge" / "dynamicmart"
            tenant_dir.mkdir(parents=True)
            (tenant_dir / "payment.md").write_text(
                "# Thanh toán\n\nKhách có thể thanh toán COD khi nhận hàng.",
                encoding="utf-8",
            )
            service = RagService(self.settings(root))
            await service.ingest()

            async def synthesized_answer(*_: object) -> str:
                return "COD cho phép khách thanh toán khi nhận hàng."

            service.chat_client.answer = synthesized_answer  # type: ignore[method-assign]
            response = await service.ask(
                ChatRequest(tenant="dynamicmart", message="Hãy so sánh phương thức COD và VNPay")
            )

            self.assertEqual("COD cho phép khách thanh toán khi nhận hàng.", response.reply)

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

    def test_rejects_unsupported_negative_claim(self) -> None:
        from app.rag.types import Chunk, SearchResult

        results = [
            SearchResult(
                Chunk(
                    "dynamicmart",
                    "1",
                    "shipping.md",
                    "Giao hàng",
                    "Đổi địa chỉ sẽ làm báo giá vận chuyển cũ không còn hợp lệ.",
                ),
                0.9,
            )
        ]

        self.assertFalse(
            RagService._negative_claims_are_grounded(
                "Không cần thanh toán trước khi đổi địa chỉ.",
                results,
            )
        )
        self.assertTrue(
            RagService._negative_claims_are_grounded(
                "Báo giá cũ không còn hợp lệ.",
                results,
            )
        )

    def test_extractive_answer_omits_heading_and_unrelated_assistant_disclaimer(self) -> None:
        from app.rag.types import Chunk, SearchResult

        results = [
            SearchResult(
                Chunk(
                    "dynamicmart",
                    "1",
                    "payment.md",
                    "Thanh toán DynamicMart",
                    "# Thanh toán DynamicMart\n\nDynamicMart hỗ trợ COD và VNPay.",
                ),
                0.9,
            ),
            SearchResult(
                Chunk(
                    "dynamicmart",
                    "2",
                    "limits.md",
                    "Giới hạn trợ lý",
                    "Trợ lý không thực hiện thanh toán hoặc thay đổi đơn hàng.",
                ),
                0.8,
            ),
        ]

        reply = RagService._retrieval_fallback("Có thể thanh toán bằng cách nào?", results)

        self.assertIn("COD và VNPay", reply)
        self.assertNotIn("Trợ lý không thực hiện", reply)
        self.assertNotIn("- Thanh toán DynamicMart", reply)
        self.assertNotIn("Theo tài liệu", reply)
        self.assertNotIn("Nguồn", reply)


if __name__ == "__main__":
    unittest.main()
