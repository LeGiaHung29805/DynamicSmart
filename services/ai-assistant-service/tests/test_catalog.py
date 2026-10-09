from __future__ import annotations

import unittest

from app.catalog import DynamicMartCatalogClient


class DynamicMartCatalogClientTest(unittest.TestCase):
    def test_detects_product_questions_without_matching_checkout_policy(self) -> None:
        self.assertTrue(DynamicMartCatalogClient.is_product_question("Tìm điện thoại dưới 20 triệu"))
        self.assertTrue(DynamicMartCatalogClient.is_product_question("Dynamic Laptop Air còn hàng không?"))
        self.assertFalse(DynamicMartCatalogClient.is_product_question("Mua ngay có làm thay đổi giỏ hàng không?"))
        self.assertFalse(DynamicMartCatalogClient.is_product_question("Phí giao hàng được tính thế nào?"))
        self.assertFalse(DynamicMartCatalogClient.is_product_question("Làm sao thêm sản phẩm yêu thích?"))
        self.assertFalse(DynamicMartCatalogClient.is_product_question("Tôi có thể đánh giá sản phẩm không?"))

    def test_extracts_keyword_price_and_sort(self) -> None:
        search = DynamicMartCatalogClient.parse_search("Tìm laptop rẻ nhất dưới 30 triệu")

        self.assertEqual("laptop", search.keyword)
        self.assertIsNone(search.minimum_price_vnd)
        self.assertEqual(30_000_000, search.maximum_price_vnd)
        self.assertEqual("PRICE_ASC", search.sort)

    def test_extracts_minimum_price(self) -> None:
        search = DynamicMartCatalogClient.parse_search("Cho tôi xem điện thoại từ 15 triệu")

        self.assertEqual("điện thoại", search.keyword)
        self.assertEqual(15_000_000, search.minimum_price_vnd)
        self.assertIsNone(search.maximum_price_vnd)


if __name__ == "__main__":
    unittest.main()
