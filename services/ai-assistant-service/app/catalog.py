from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any
from urllib.parse import quote

import httpx

from app.schemas import ProductInput


@dataclass(frozen=True)
class CatalogSearch:
    keyword: str | None
    minimum_price_vnd: int | None
    maximum_price_vnd: int | None
    sort: str


class DynamicMartCatalogClient:
    """Read current public product data through Catalog Service, never its database."""

    _POLICY_TERMS = (
        "mua ngay", "thanh toán", "checkout", "đơn hàng", "giao hàng",
        "vận chuyển", "voucher", "mã giảm", "đổi trả", "hoàn tiền", "tài khoản",
        "yêu thích", "wishlist", "đánh giá", "review", "hỏi đáp", "câu hỏi sản phẩm",
    )
    _PRODUCT_TERMS = (
        "sản phẩm", "điện thoại", "laptop", "máy tính", "tablet", "tai nghe",
        "đồng hồ", "tivi", "phone", "còn hàng", "giảm giá", "khuyến mãi", "giá bao nhiêu",
    )
    _PRODUCT_ACTIONS = ("tìm", "cho tôi xem", "gợi ý", "có mẫu", "có loại", "có bán")
    _STOP_PHRASES = (
        "cho tôi xem", "cho tôi", "hãy", "tìm kiếm", "tìm", "gợi ý", "sản phẩm",
        "mặt hàng", "đang bán", "đang có", "còn hàng", "giảm giá", "khuyến mãi",
        "giá bao nhiêu", "giá", "bao nhiêu", "có mẫu", "có loại", "có bán",
        "rẻ nhất", "giá thấp", "tăng dần", "đắt nhất", "giá cao", "giảm dần",
        "bán chạy", "mới nhất", "có", "nào", "không", "vậy",
    )

    def __init__(self, base_url: str, storefront_url: str, timeout_seconds: float):
        self.base_url = base_url.rstrip("/")
        self.storefront_url = storefront_url.rstrip("/")
        self.client = httpx.AsyncClient(timeout=timeout_seconds)

    async def products_for_question(self, question: str) -> list[ProductInput] | None:
        if not self.is_product_question(question):
            return None

        search = self.parse_search(question)
        products = await self._search(search)
        if not products and search.keyword:
            products = await self._search(search, brand=search.keyword)
        return [self._to_product(item) for item in products[:6]]

    @classmethod
    def is_product_question(cls, question: str) -> bool:
        normalized = question.casefold()
        if any(term in normalized for term in cls._POLICY_TERMS):
            return False
        if any(term in normalized for term in cls._PRODUCT_TERMS):
            return True
        return any(term in normalized for term in cls._PRODUCT_ACTIONS)

    @classmethod
    def parse_search(cls, question: str) -> CatalogSearch:
        normalized = " ".join(question.casefold().split())
        minimum, maximum = cls._price_bounds(normalized)
        sort = "NEWEST"
        if any(term in normalized for term in ("rẻ nhất", "giá thấp", "tăng dần")):
            sort = "PRICE_ASC"
        elif any(term in normalized for term in ("đắt nhất", "giá cao", "giảm dần")):
            sort = "PRICE_DESC"
        elif "bán chạy" in normalized:
            sort = "BEST_SELLER"

        keyword = re.sub(
            r"\b(?:dưới|tối đa|không quá|trên|tối thiểu|từ)\s+\d+(?:[.,]\d+)?\s*(?:triệu|tr|nghìn|ngàn|k|đ|vnd)?\b",
            " ",
            normalized,
        )
        for phrase in sorted(cls._STOP_PHRASES, key=len, reverse=True):
            keyword = re.sub(rf"(?<!\w){re.escape(phrase)}(?!\w)", " ", keyword)
        keyword = re.sub(r"[^\w\s-]", " ", keyword, flags=re.UNICODE)
        keyword = " ".join(keyword.split()).strip(" -")
        return CatalogSearch(keyword or None, minimum, maximum, sort)

    @staticmethod
    def _price_bounds(question: str) -> tuple[int | None, int | None]:
        pattern = re.compile(
            r"(?P<operator>dưới|tối đa|không quá|trên|tối thiểu|từ)\s+"
            r"(?P<amount>\d+(?:[.,]\d+)?)\s*(?P<unit>triệu|tr|nghìn|ngàn|k|đ|vnd)?"
        )
        minimum = maximum = None
        for match in pattern.finditer(question):
            amount = float(match.group("amount").replace(",", "."))
            unit = match.group("unit") or ""
            multiplier = 1_000_000 if unit in {"triệu", "tr"} else 1_000 if unit in {"nghìn", "ngàn", "k"} else 1
            value = max(0, int(amount * multiplier))
            if match.group("operator") in {"dưới", "tối đa", "không quá"}:
                maximum = value
            else:
                minimum = value
        return minimum, maximum

    async def _search(self, search: CatalogSearch, brand: str | None = None) -> list[dict[str, Any]]:
        params: list[tuple[str, str | int]] = [("page", 0), ("size", 6), ("sort", search.sort)]
        if search.keyword and brand is None:
            params.append(("keyword", search.keyword))
        if search.minimum_price_vnd is not None:
            params.append(("minimumPriceVnd", search.minimum_price_vnd))
        if search.maximum_price_vnd is not None:
            params.append(("maximumPriceVnd", search.maximum_price_vnd))
        if brand:
            params.append(("attribute", f"BRAND:{brand}"))

        response = await self.client.get(f"{self.base_url}/products", params=params)
        response.raise_for_status()
        payload = response.json()
        content = payload.get("data", {}).get("content", []) if isinstance(payload, dict) else []
        return [item for item in content if isinstance(item, dict)]

    def _to_product(self, item: dict[str, Any]) -> ProductInput:
        price = item.get("representativePrice") or {}
        sale_price = price.get("salePriceVnd")
        list_price = price.get("listPriceVnd")
        category = item.get("category") or {}
        image = item.get("primaryImage") or {}
        slug = str(item.get("slug") or "")
        description = str(item.get("shortDescription") or "").strip()
        category_name = str(category.get("name") or "Sản phẩm")
        detail = f"{category_name} · {description}" if description else category_name
        return ProductInput(
            id=item.get("id"),
            slug=slug,
            name=str(item.get("name") or slug),
            detail=detail[:500],
            url=f"{self.storefront_url}/products/{quote(slug)}",
            image_url=image.get("imageUrl"),
            price=int(sale_price if sale_price is not None else list_price) if (sale_price is not None or list_price is not None) else None,
            compare_at_price=int(list_price) if sale_price is not None and list_price is not None else None,
            discount_percentage=int(price.get("directSalePercent") or 0),
            stock_total=0,
            in_stock=item.get("inStock") is True,
        )

    async def close(self) -> None:
        await self.client.aclose()
