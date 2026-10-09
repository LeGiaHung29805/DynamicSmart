from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from app.rag.embeddings import HashEmbeddingProvider
from app.rag.loader import DirectoryDocumentLoader
from app.rag.splitter import RecursiveTextSplitter
from app.rag.store import QdrantVectorStore, score_lexical_match
from app.rag.types import Chunk, Document


class RagPipelineTest(unittest.IsolatedAsyncioTestCase):
    def test_loader_separates_documents_by_tenant(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant = root / "fashion-ecommerce"
            tenant.mkdir()
            (tenant / "faq.md").write_text("# Thanh toán\n\nCửa hàng hỗ trợ COD.", encoding="utf-8")

            documents = DirectoryDocumentLoader(root).load()

            self.assertEqual(1, len(documents))
            self.assertEqual("fashion-ecommerce", documents[0].tenant)
            self.assertEqual("Thanh toán", documents[0].title)

    def test_loader_ignores_legacy_catalog_snapshot(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            tenant = root / "dynamicmart"
            tenant.mkdir()
            (tenant / "faq.md").write_text("# FAQ\n\nThông tin ổn định.", encoding="utf-8")
            (tenant / "danh-muc-va-san-pham-chi-tiet.md").write_text(
                "# Snapshot\n\nGiá và tồn kho cũ.", encoding="utf-8"
            )

            documents = DirectoryDocumentLoader(root).load()

            self.assertEqual(["dynamicmart/faq.md"], [document.source for document in documents])

    def test_recursive_splitter_applies_overlap_and_stable_ids(self) -> None:
        document = Document(
            tenant="dynamicmart",
            source="dynamicmart/test.md",
            title="Test",
            content=("Đoạn nội dung kiểm thử. " * 80).strip(),
        )
        splitter = RecursiveTextSplitter(chunk_size=220, chunk_overlap=40)

        first = splitter.split_documents([document])
        second = splitter.split_documents([document])

        self.assertGreater(len(first), 1)
        self.assertEqual([chunk.chunk_id for chunk in first], [chunk.chunk_id for chunk in second])
        self.assertTrue(all(len(chunk.content) <= 220 for chunk in first))
        self.assertTrue(all(not chunk.content.startswith("oạn") for chunk in first))

    async def test_hash_embedding_and_qdrant_retrieval_are_tenant_scoped(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            store = QdrantVectorStore("test_knowledge")
            splitter = RecursiveTextSplitter(chunk_size=500, chunk_overlap=50)
            provider = HashEmbeddingProvider()
            fashion_chunks = splitter.split_documents([
                Document("fashion-ecommerce", "fashion/payment.md", "Thanh toán", "Thanh toán COD khi nhận hàng."),
            ])
            mart_chunks = splitter.split_documents([
                Document("dynamicmart", "mart/shipping.md", "Vận chuyển", "GHN cung cấp báo giá vận chuyển."),
            ])
            store.replace_tenant(
                "fashion-ecommerce",
                fashion_chunks,
                await provider.embed([chunk.content for chunk in fashion_chunks]),
                provider.name,
            )
            store.replace_tenant(
                "dynamicmart",
                mart_chunks,
                await provider.embed([chunk.content for chunk in mart_chunks]),
                provider.name,
            )

            query = (await provider.embed(["thanh toán COD"]))[0]
            results = store.search("fashion-ecommerce", query, 3)

            self.assertEqual(1, len(results))
            self.assertEqual("Thanh toán", results[0].chunk.title)
            self.assertGreater(results[0].score, 0)
            self.assertEqual({"dynamicmart": 1, "fashion-ecommerce": 1}, store.counts_by_tenant())

    def test_hybrid_search_prioritizes_exact_vietnamese_policy_terms(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            store = QdrantVectorStore("test_knowledge")
            chunks = [
                Chunk("fashion-ecommerce", "1", "products.md", "Sản phẩm", "Thông tin sản phẩm còn hàng."),
                Chunk("fashion-ecommerce", "2", "returns.md", "Đổi trả", "Chính sách đổi trả cần nhân viên xác nhận."),
            ]
            store.replace_tenant("fashion-ecommerce", chunks, [[1.0, 0.0], [1.0, 0.0]], "test")

            results = store.search(
                "fashion-ecommerce",
                [1.0, 0.0],
                2,
                query_text="Tôi muốn hỏi chính sách đổi trả",
            )

            self.assertEqual("returns.md", results[0].chunk.source)

    def test_shipping_question_prioritizes_quote_answer_over_assistant_disclaimer(self) -> None:
        question = "Phí vận chuyển được tính như thế nào?"
        answer = "Phí giao hàng được máy chủ báo giá qua GHN từ một kho gửi cố định."
        disclaimer = "Trợ lý không phải nguồn quyết định cho phí vận chuyển hoặc trạng thái đơn."

        self.assertGreater(
            score_lexical_match(question, answer),
            score_lexical_match(question, disclaimer),
        )

    def test_wishlist_question_does_not_match_review_sentence_because_of_question_words(self) -> None:
        question = "Làm sao thêm sản phẩm yêu thích?"
        answer = "Khách có thể thêm sản phẩm vào wishlist của tài khoản."
        unrelated = "Đánh giá có thể gồm số sao, nội dung và URL ảnh hợp lệ."

        self.assertGreater(
            score_lexical_match(question, answer),
            score_lexical_match(question, unrelated),
        )

    def test_search_diversifies_sources(self) -> None:
        with tempfile.TemporaryDirectory():
            store = QdrantVectorStore("test_diversity")
            chunks = [
                Chunk("dynamicmart", "1", "payment.md", "Thanh toán", "Thanh toán COD."),
                Chunk("dynamicmart", "2", "payment.md", "Thanh toán", "Thanh toán VNPay."),
                Chunk("dynamicmart", "3", "payment.md", "Thanh toán", "Thanh toán PayOS."),
                Chunk("dynamicmart", "4", "orders.md", "Đơn hàng", "Đơn lưu trạng thái thanh toán."),
            ]
            store.replace_tenant("dynamicmart", chunks, [[1.0, 0.0]] * 4, "test")

            results = store.search("dynamicmart", [1.0, 0.0], 4, query_text="thanh toán")

            self.assertLessEqual(sum(result.chunk.source == "payment.md" for result in results), 2)
            self.assertIn("orders.md", {result.chunk.source for result in results})


if __name__ == "__main__":
    unittest.main()
