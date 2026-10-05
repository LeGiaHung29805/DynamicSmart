from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from app.rag.embeddings import HashEmbeddingProvider
from app.rag.loader import DirectoryDocumentLoader
from app.rag.splitter import RecursiveTextSplitter
from app.rag.store import QdrantVectorStore
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


if __name__ == "__main__":
    unittest.main()
