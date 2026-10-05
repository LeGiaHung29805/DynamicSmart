# Trợ lý AI RAG dùng chung

## Phạm vi

MVP trợ lý AI là P2 và không thay đổi nguồn sự thật của Catalog, Cart, Order, Payment hoặc Shipping. Service đặt tại `services/ai-assistant-service` và được dùng chung với dự án Fashion Ecommerce bằng trường `tenant`.

DynamicMart dùng tenant `dynamicmart`; Fashion Ecommerce dùng tenant `fashion-ecommerce`. Vector và kết quả truy xuất luôn được lọc theo tenant để không trộn tài liệu hai dự án.

## Pipeline theo video tham khảo

```text
Document loader (.md, .txt, .pdf)
→ Recursive text splitter
→ Ollama embedding
→ Qdrant vector database (tenant payload filter)
→ hybrid retriever (semantic + từ khóa tiếng Việt)
→ context + câu hỏi
→ Ollama chat model
→ câu trả lời + citations
```

Mô hình mặc định là `qwen2.5:0.5b`; embedding mặc định là `nomic-embed-text`. Khi Ollama không sẵn sàng trong môi trường phát triển, embedding băm chỉ được dùng để kiểm thử luồng và phần trả lời chuyển sang trích đoạn nguồn, không được xem là cấu hình production.

## Contract

Frontend gọi Gateway:

```text
POST /api/v1/assistant/chat
```

Body:

```json
{"tenant":"dynamicmart","message":"Phí vận chuyển tính thế nào?","products":[]}
```

Response gồm `reply`, `intent`, `query`, `products` và `citations`. MVP DynamicMart chỉ dùng knowledge RAG; chưa truyền dữ liệu Catalog thời gian thực vào `products`. Khi bổ sung tìm sản phẩm, Adapter phía máy chủ phải lấy dữ liệu từ Catalog Service và không tin giá/tồn kho do trình duyệt gửi.

## Chạy local

1. Cài và chạy Ollama, tải `qwen2.5:0.5b` và `nomic-embed-text`.
2. Cài dependency và chạy FastAPI theo `services/ai-assistant-service/README.md`.
3. Gateway dùng `AI_ASSISTANT_SERVICE_URL=http://127.0.0.1:8001` theo mặc định.
4. Chạy Gateway và frontend như quy trình hiện có.

Môi trường phát triển chạy Qdrant Server bằng `docker compose up -d qdrant` trong
thư mục service, mặc định tại `http://127.0.0.1:6333`. Production đặt
`QDRANT_URL`, `QDRANT_API_KEY` và `QDRANT_COLLECTION` để kết nối Qdrant
Server/Cloud. Mỗi point lưu `tenant` trong payload và mọi truy vấn đều áp dụng
filter tenant phía Qdrant. Chế độ Qdrant in-memory chỉ dùng cho unit test.

Không đưa secret, dữ liệu thanh toán hoặc đơn hàng cá nhân vào `knowledge/`. Dữ liệu cá nhân trong tương lai phải đi qua API đã xác thực và kiểm tra ownership, không đưa vào vector store dùng chung.
