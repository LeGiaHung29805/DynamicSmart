from __future__ import annotations

import asyncio
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
        "đồ văn phòng", "hàng văn phòng", "hàng về", "hàng cho", "hàng dùng cho",
    )
    _PRODUCT_ACTIONS = (
        "tìm", "cho tôi xem", "gợi ý", "có mẫu", "có loại", "có bán",
        "các hàng", "những hàng", "các mặt hàng", "những mặt hàng",
        "các đồ", "những đồ",
    )
    _STOP_PHRASES = (
        "các loại sản phẩm dùng cho", "những loại sản phẩm dùng cho",
        "các loại sản phẩm về", "những loại sản phẩm về",
        "các loại sản phẩm cho", "những loại sản phẩm cho",
        "loại sản phẩm dùng cho", "loại sản phẩm về", "loại sản phẩm cho",
        "các loại sản phẩm", "những loại sản phẩm", "loại sản phẩm",
        "các mặt hàng dùng cho", "những mặt hàng dùng cho",
        "các mặt hàng về", "những mặt hàng về",
        "các mặt hàng cho", "những mặt hàng cho",
        "các hàng dùng cho", "những hàng dùng cho",
        "các hàng về", "những hàng về", "các hàng cho", "những hàng cho",
        "các đồ dùng cho", "những đồ dùng cho",
        "các đồ về", "những đồ về", "các đồ cho", "những đồ cho",
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
        if products and search.keyword:
            products = self._relevant_products(products, search.keyword)
        if not products and search.keyword:
            category_id = await self._category_id_for_keyword(search.keyword)
            if category_id:
                products = await self._search(search, category_id=category_id)
        if not products and search.keyword:
            products = await self._search(search, brand=search.keyword)
        return [self._to_product(item) for item in products[:6]]

    async def products_for_index(self) -> list[dict[str, Any]]:
        """Return all public product summaries enriched with category meaning."""
        categories_response = await self.client.get(f"{self.base_url}/categories")
        categories_response.raise_for_status()
        categories_payload = categories_response.json()
        categories = categories_payload.get("data", []) if isinstance(categories_payload, dict) else []
        category_descriptions = {
            str(category.get("id")): str(category.get("description") or "")
            for category in self._flatten_categories(categories if isinstance(categories, list) else [])
            if category.get("id")
        }

        products: list[dict[str, Any]] = []
        page = 0
        while True:
            response = await self.client.get(
                f"{self.base_url}/products",
                params={"page": page, "size": 100, "sort": "NEWEST"},
            )
            response.raise_for_status()
            payload = response.json()
            data = payload.get("data", {}) if isinstance(payload, dict) else {}
            content = data.get("content", []) if isinstance(data, dict) else []
            for item in content:
                if not isinstance(item, dict) or not item.get("id"):
                    continue
                product = dict(item)
                category = product.get("category") or {}
                product["categoryDescription"] = category_descriptions.get(str(category.get("id")), "")
                products.append(product)
            if not isinstance(data, dict) or data.get("last") is True:
                break
            total_pages = int(data.get("totalPages") or 0)
            page += 1
            if total_pages and page >= total_pages:
                break
        return products

    async def products_by_ids(self, product_ids: list[str]) -> list[ProductInput]:
        async def fetch(product_id: str) -> ProductInput | None:
            response = await self.client.get(f"{self.base_url}/products/id/{product_id}")
            if response.status_code == 404:
                return None
            response.raise_for_status()
            payload = response.json()
            item = payload.get("data") if isinstance(payload, dict) else None
            return self._to_product_detail(item) if isinstance(item, dict) else None

        resolved = await asyncio.gather(*(fetch(product_id) for product_id in product_ids))
        return [product for product in resolved if product is not None]

    @staticmethod
    def product_document_text(item: dict[str, Any]) -> str:
        category = item.get("category") or {}
        parts = (
            f"Tên sản phẩm: {item.get('name') or ''}",
            f"Danh mục: {category.get('name') or ''}",
            f"Mô tả: {item.get('shortDescription') or ''}",
            f"Ý nghĩa danh mục: {item.get('categoryDescription') or ''}",
        )
        return ". ".join(part for part in parts if part.rsplit(":", 1)[-1].strip())

    @classmethod
    def is_product_question(cls, question: str) -> bool:
        normalized = question.casefold()
        if cls.is_policy_question(question):
            return False
        if any(term in normalized for term in cls._PRODUCT_TERMS):
            return True
        return any(term in normalized for term in cls._PRODUCT_ACTIONS)

    @classmethod
    def is_policy_question(cls, question: str) -> bool:
        normalized = question.casefold()
        return any(term in normalized for term in cls._POLICY_TERMS)

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

    async def _search(
        self,
        search: CatalogSearch,
        brand: str | None = None,
        category_id: str | None = None,
    ) -> list[dict[str, Any]]:
        params: list[tuple[str, str | int]] = [("page", 0), ("size", 6), ("sort", search.sort)]
        if search.keyword and brand is None and category_id is None:
            params.append(("keyword", search.keyword))
        if category_id:
            params.append(("categoryId", category_id))
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

    async def _category_id_for_keyword(self, keyword: str) -> str | None:
        response = await self.client.get(f"{self.base_url}/categories")
        response.raise_for_status()
        payload = response.json()
        categories = payload.get("data", []) if isinstance(payload, dict) else []
        return self._matching_category_id(categories, keyword)

    @classmethod
    def _matching_category_id(cls, categories: Any, keyword: str) -> str | None:
        query = cls._searchable_text(keyword)
        query_tokens = set(query.split())
        if not query_tokens or not isinstance(categories, list):
            return None

        for category in cls._flatten_categories(categories):
            searchable = cls._searchable_text(" ".join(
                str(category.get(field) or "")
                for field in ("name", "slug", "description")
            ))
            if query in searchable or query_tokens.issubset(set(searchable.split())):
                category_id = category.get("id")
                if category_id:
                    return str(category_id)
        return None

    @classmethod
    def _flatten_categories(cls, categories: list[Any]) -> list[dict[str, Any]]:
        flattened: list[dict[str, Any]] = []
        for category in categories:
            if not isinstance(category, dict):
                continue
            flattened.append(category)
            children = category.get("children")
            if isinstance(children, list):
                flattened.extend(cls._flatten_categories(children))
        return flattened

    @staticmethod
    def _searchable_text(value: str) -> str:
        return " ".join(re.sub(r"[^\w]+", " ", value.casefold(), flags=re.UNICODE).split())

    @classmethod
    def _relevant_products(cls, products: list[dict[str, Any]], keyword: str) -> list[dict[str, Any]]:
        keyword_tokens = set(cls._searchable_text(keyword).split())
        if not keyword_tokens:
            return products

        relevant: list[dict[str, Any]] = []
        for product in products:
            category = product.get("category") or {}
            searchable = cls._searchable_text(" ".join((
                str(product.get("name") or ""),
                str(product.get("shortDescription") or ""),
                str(category.get("name") or ""),
            )))
            if keyword_tokens.issubset(set(searchable.split())):
                relevant.append(product)
        return relevant

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

    def _to_product_detail(self, item: dict[str, Any]) -> ProductInput:
        variants = [
            variant for variant in (item.get("variants") or [])
            if isinstance(variant, dict) and variant.get("status") == "ACTIVE"
        ]
        purchasable = [variant for variant in variants if variant.get("purchasable") is True]
        candidates = purchasable or variants

        def effective_price(variant: dict[str, Any]) -> int:
            price = variant.get("price") or {}
            value = price.get("salePriceVnd")
            if value is None:
                value = price.get("listPriceVnd")
            return int(value) if value is not None else 2**63 - 1

        representative = min(candidates, key=effective_price) if candidates else {}
        price = representative.get("price") or {}
        sale_price = price.get("salePriceVnd")
        list_price = price.get("listPriceVnd")
        images = [image for image in (item.get("images") or []) if isinstance(image, dict)]
        primary_image = next((image for image in images if image.get("primary") is True), images[0] if images else {})
        stock_total = sum(
            max(0, int((variant.get("inventory") or {}).get("availableQuantity") or 0))
            for variant in variants
        )
        category = item.get("category") or {}
        description = str(item.get("shortDescription") or "").strip()
        category_name = str(category.get("name") or "Sản phẩm")
        slug = str(item.get("slug") or "")
        return ProductInput(
            id=item.get("id"),
            slug=slug,
            name=str(item.get("name") or slug),
            detail=(f"{category_name} · {description}" if description else category_name)[:500],
            url=f"{self.storefront_url}/products/{quote(slug)}",
            image_url=primary_image.get("imageUrl"),
            price=int(sale_price if sale_price is not None else list_price)
            if sale_price is not None or list_price is not None else None,
            compare_at_price=int(list_price) if sale_price is not None and list_price is not None else None,
            discount_percentage=int(price.get("directSalePercent") or 0),
            stock_total=stock_total,
            in_stock=bool(purchasable) and stock_total > 0,
        )

    async def close(self) -> None:
        await self.client.aclose()
