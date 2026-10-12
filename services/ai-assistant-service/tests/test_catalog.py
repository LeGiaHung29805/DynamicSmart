from __future__ import annotations

import unittest

from app.catalog import DynamicMartCatalogClient


class DynamicMartCatalogClientTest(unittest.TestCase):
    def test_detects_product_questions_without_matching_checkout_policy(self) -> None:
        self.assertTrue(DynamicMartCatalogClient.is_product_question("Tìm điện thoại dưới 20 triệu"))
        self.assertTrue(DynamicMartCatalogClient.is_product_question("Dynamic Laptop Air còn hàng không?"))
        self.assertTrue(DynamicMartCatalogClient.is_product_question("Các hàng về văn phòng"))
        self.assertTrue(DynamicMartCatalogClient.is_product_question("Những đồ dùng cho nhà bếp"))
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

    def test_extracts_keyword_from_natural_product_group_phrase(self) -> None:
        office = DynamicMartCatalogClient.parse_search("Các hàng về văn phòng")
        kitchen = DynamicMartCatalogClient.parse_search("Những đồ dùng cho nhà bếp")

        self.assertEqual("văn phòng", office.keyword)
        self.assertEqual("nhà bếp", kitchen.keyword)

    def test_matches_keyword_against_nested_or_top_level_category(self) -> None:
        categories = [
            {
                "id": "electronics",
                "name": "Điện tử",
                "children": [
                    {"id": "laptop", "name": "Laptop", "children": []},
                ],
            },
            {
                "id": "office",
                "name": "Sách & văn phòng",
                "slug": "seed-sach-van-phong",
                "description": "Sách và dụng cụ văn phòng",
                "children": [],
            },
        ]

        self.assertEqual(
            "office",
            DynamicMartCatalogClient._matching_category_id(categories, "văn phòng"),
        )
        self.assertEqual(
            "laptop",
            DynamicMartCatalogClient._matching_category_id(categories, "laptop"),
        )
        self.assertIsNone(
            DynamicMartCatalogClient._matching_category_id(categories, "nhà bếp"),
        )


if __name__ == "__main__":
    unittest.main()
