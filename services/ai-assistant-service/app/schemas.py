from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ProductInput(BaseModel):
    model_config = ConfigDict(extra="ignore")

    id: int | str | None = None
    slug: str | None = None
    name: str = Field(min_length=1, max_length=255)
    detail: str | None = Field(default=None, max_length=500)
    url: str
    image_url: str | None = None
    price: int | None = Field(default=None, ge=0)
    compare_at_price: int | None = Field(default=None, ge=0)
    discount_percentage: int | None = Field(default=None, ge=0, le=100)
    stock_total: int = Field(default=0, ge=0)
    in_stock: bool = False

    @field_validator("url")
    @classmethod
    def validate_product_url(cls, value: str) -> str:
        if not value.startswith(("http://", "https://")):
            raise ValueError("URL sản phẩm phải dùng HTTP hoặc HTTPS.")
        return value

    @field_validator("image_url")
    @classmethod
    def validate_image_url(cls, value: str | None) -> str | None:
        if value is not None and not value.startswith(("/", "http://", "https://")):
            raise ValueError("URL ảnh không hợp lệ.")
        return value


class ChatTurn(BaseModel):
    model_config = ConfigDict(extra="forbid")

    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=800)

    @field_validator("content")
    @classmethod
    def normalize_content(cls, value: str) -> str:
        normalized = " ".join(value.split())
        if not normalized:
            raise ValueError("Nội dung lịch sử không được để trống.")
        return normalized

class ChatRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    message: str = Field(min_length=2, max_length=500)
    tenant: str = Field(default="fashion-ecommerce", pattern=r"^[a-z0-9][a-z0-9-]{1,63}$")
    products: list[ProductInput] = Field(default_factory=list, max_length=6)
    history: list[ChatTurn] = Field(default_factory=list, max_length=10)

    @field_validator("message")
    @classmethod
    def normalize_message(cls, value: str) -> str:
        normalized = " ".join(value.split())
        if len(normalized) < 2:
            raise ValueError("Câu hỏi phải có ít nhất 2 ký tự.")
        return normalized


class Citation(BaseModel):
    source: str
    title: str
    excerpt: str
    score: float = Field(ge=-1, le=1)


class ChatResponse(BaseModel):
    reply: str
    intent: str
    query: str
    products: list[dict[str, Any]]
    citations: list[Citation] = Field(default_factory=list)


class HealthResponse(BaseModel):
    status: str
    embedding_provider: str
    embedding_model: str
    chat_model: str
    llm_synthesis: bool
    vector_store: str
    vector_store_mode: str
    tenants: dict[str, int]
