from __future__ import annotations

import unittest

from app.product_store import ProductDocument, ProductVectorStore


class ProductVectorStoreTest(unittest.TestCase):
    def test_replaces_searches_and_tracks_catalog_fingerprint(self) -> None:
        store = ProductVectorStore("test_products", None, None)
        documents = [
            ProductDocument("shirt", "Áo thời trang mặc đi làm", "catalog-v1"),
            ProductDocument("headphones", "Tai nghe Bluetooth nghe nhạc", "catalog-v1"),
        ]

        store.replace(documents, [[1.0, 0.0], [0.0, 1.0]])
        hits = store.search([1.0, 0.0], "trang phục đi làm", limit=2)

        self.assertEqual(2, store.count())
        self.assertEqual("catalog-v1", store.fingerprint())
        self.assertEqual("shirt", hits[0].product_id)
        self.assertGreater(hits[0].score, hits[1].score)
        store.close()


if __name__ == "__main__":
    unittest.main()
