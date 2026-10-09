"""Script đồng bộ dữ liệu sản phẩm từ Catalog API vào kho tri thức của AI Assistant."""

from __future__ import annotations

import json
from pathlib import Path
import httpx

CATALOG_API_URL = "http://localhost:8080/api/v1"
KNOWLEDGE_DIR = Path(__file__).resolve().parent / "knowledge" / "dynamicmart"
OUTPUT_FILE = KNOWLEDGE_DIR / "danh-muc-va-san-pham-chi-tiet.md"


def format_currency(amount: int | None) -> str:
    if amount is None:
        return "Chưa cập nhật"
    return f"{amount:,.0f} VNĐ".replace(",", ".")


def sync_catalog():
    print(f"Connecting to Catalog API at {CATALOG_API_URL}...")
    with httpx.Client(base_url=CATALOG_API_URL, timeout=10.0) as client:
        # 1. Lấy danh mục
        cat_resp = client.get("/catalog/categories")
        categories_data = cat_resp.json().get("data", [])

        # 2. Lấy danh sách sản phẩm
        prod_resp = client.get("/catalog/products")
        products_content = prod_resp.json().get("data", {}).get("content", [])

        # 3. Lấy chi tiết từng sản phẩm
        products_detail = []
        for p in products_content:
            slug = p["slug"]
            detail_resp = client.get(f"/catalog/products/{slug}")
            products_detail.append(detail_resp.json().get("data", {}))

    md_lines = [
        "# Danh mục và Sản phẩm chi tiết tại DynamicMart",
        "",
        "DynamicMart là nền tảng thương mại điện tử B2C trực tiếp, cung cấp các sản phẩm công nghệ chính hãng chất lượng cao. Dưới đây là dữ liệu sản phẩm và danh mục được cập nhật tự động từ hệ thống:",
        "",
        "## 1. Hệ thống Danh mục sản phẩm (Categories)",
        "",
    ]

    for cat in categories_data:
        md_lines.append(f"- **{cat.get('name')} (Mã: {cat.get('code')})**: {cat.get('description', '')}")
        for child in cat.get("children", []):
            md_lines.append(f"  - **{child.get('name')} (Mã: {child.get('code')})**: {child.get('description', '')}")

    md_lines.extend([
        "",
        "---",
        "",
        "## 2. Danh sách Sản phẩm và Biến thể chi tiết",
        "",
    ])

    for p in products_detail:
        p_name = p.get("name", "")
        p_slug = p.get("slug", "")
        p_desc = p.get("shortDescription") or p.get("description", "")
        p_cat = p.get("category", {}).get("name", "")
        brand = "Chưa rõ"
        for attr in p.get("attributes", []):
            if attr.get("attributeCode") == "BRAND":
                brand = attr.get("value", "")

        md_lines.append(f"### Sản phẩm: {p_name} (Mã: {p_slug})")
        md_lines.append(f"- **Danh mục:** {p_cat}")
        md_lines.append(f"- **Thương hiệu:** {brand}")
        md_lines.append(f"- **Mô tả:** {p_desc}")
        md_lines.append("- **Các phiên bản / Biến thể (Variants):**")

        for v in p.get("variants", []):
            sku = v.get("sku", "")
            v_name = v.get("name", "")
            price_info = v.get("price", {})
            list_price = price_info.get("listPriceVnd")
            sale_price = price_info.get("salePriceVnd")
            discount_vnd = price_info.get("directSaleDiscountVnd", 0)
            percent = price_info.get("directSalePercent", 0)
            weight = v.get("weightGrams", 0)
            stock = v.get("inventory", {}).get("availableQuantity", 0)

            variant_desc = f"  * **Phiên bản {v_name} (Mã SKU: {sku}):**\n"
            variant_desc += f"    - Giá niêm yết: {format_currency(list_price)}\n"
            if sale_price and discount_vnd > 0:
                variant_desc += f"    - Giá khuyến mãi: **{format_currency(sale_price)}** (Giảm {format_currency(discount_vnd)}, tương đương {percent}%)\n"
            else:
                variant_desc += f"    - Giá bán: **{format_currency(list_price)}**\n"
            variant_desc += f"    - Số lượng tồn kho: {stock} sản phẩm sẵn sàng giao\n"
            variant_desc += f"    - Trọng lượng đóng gói: {weight} gram\n"
            md_lines.append(variant_desc)

    md_lines.extend([
        "---",
        "",
        "## 3. Chính sách Khuyến mãi và Mã giảm giá (Vouchers)",
        "",
        "- **Mã DYNAMIC10**: Giảm ngay 10% tổng giá trị đơn hàng cho đơn từ 500.000 VNĐ trở lên, mức giảm tối đa là 200.000 VNĐ.",
        "- **Mã FREESHIP50**: Giảm tối đa 50.000 VNĐ phí vận chuyển cho đơn hàng có giá trị từ 300.000 VNĐ trở lên.",
        "- **Quy tắc**: Giá giảm trực tiếp trên sản phẩm (Direct Sale) được tự động trừ trước, sau đó khách hàng có thể áp dụng thêm 1 voucher đơn hàng và 1 voucher phí giao hàng.",
        "",
    ])

    KNOWLEDGE_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_FILE.write_text("\n".join(md_lines), encoding="utf-8")
    print(f"Sync completed! Written knowledge to {OUTPUT_FILE}")


if __name__ == "__main__":
    sync_catalog()
