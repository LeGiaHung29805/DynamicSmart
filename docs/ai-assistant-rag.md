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

Luồng runtime được tối ưu theo loại câu hỏi:

```text
Tìm sản phẩm -> Catalog Service -> response trực tiếp
FAQ/chính sách đơn giản -> embedding + Qdrant -> trích xuất có citation
Câu hỏi so sánh/tổng hợp/tư vấn -> embedding + Qdrant -> Ollama streaming (khi bật synthesis)
```

FastAPI tái sử dụng kết nối HTTP, cache tối đa 128 embedding câu hỏi lặp lại và giữ model
Ollama nóng trong 30 phút. Chat model mặc định dùng context 2048 token, sinh tối đa 160 token.
Do `qwen2.5:0.5b` có thể diễn giải sai chính sách, môi trường mặc định đặt
`RAG_ENABLE_LLM_SYNTHESIS=false`: câu trả lời chính sách được trích xuất có nguồn. Chỉ bật
LLM synthesis khi đã chuyển sang model mạnh hơn và kiểm thử bộ câu hỏi nghiệp vụ.

Retriever dùng kết hợp semantic score và từ khóa tiếng Việt đã chuẩn hóa từ đồng nghĩa. Kết
quả được giới hạn tối đa hai chunk mỗi file để context không bị một tài liệu chiếm hết. Splitter
ưu tiên biên đoạn và câu, overlap theo đơn vị hoàn chỉnh; câu trả lời extractive chọn tối đa ba
ý có liên quan và loại tiêu đề/câu giới hạn trợ lý không trả lời trực tiếp câu hỏi.

Mô hình chat mặc định là `qwen2.5:0.5b`; embedding mặc định là
`qwen3-embedding:0.6b`. Collection mặc định `ecommerce_knowledge_qwen3_06b` được tách khỏi
collection của model embedding cũ để không trộn các vector khác kích thước. Khi Ollama không
sẵn sàng trong môi trường phát triển, embedding băm chỉ được dùng để kiểm thử luồng và phần
trả lời chuyển sang trích đoạn nguồn, không được xem là cấu hình production.

## Contract

Frontend gọi Gateway:

```text
POST /api/v1/assistant/chat
```

Giao diện DynamicMart dùng `POST /api/v1/assistant/chat/stream` (NDJSON) để hiển thị token
ngay khi Ollama sinh nội dung. Endpoint JSON cũ tiếp tục được giữ cho client hiện tại và
Fashion Ecommerce.

Frontend chỉ trình bày nội dung trả lời và thẻ sản phẩm, giữ xuống dòng giữa các ý và không
hiển thị citation. Trường `citations` vẫn được giữ trong contract làm metadata kỹ thuật để
không phá vỡ client cũ.

Body:

```json
{
  "tenant":"dynamicmart",
  "message":"Nếu đổi địa chỉ thì sao?",
  "products":[],
  "history":[
    {"role":"user","content":"Phí vận chuyển tính thế nào?"},
    {"role":"assistant","content":"Phí được hiển thị trước khi xác nhận."}
  ]
}
```

`history` là trường tùy chọn, tối đa 10 lượt (`user`/`assistant`), mỗi nội dung tối đa 800 ký
tự. Nó chỉ được frontend giữ trong phiên trang hiện tại và gửi kèm request để giải nghĩa câu hỏi
nối tiếp. FastAPI không lưu lịch sử vào database hoặc Qdrant; nội dung nhạy cảm phổ biến bị
lược bỏ trước khi dùng cho retrieval/prompt.

Response gồm `reply`, `intent`, `query`, `products` và `citations`. Với câu hỏi tìm sản phẩm,
AI Assistant tự lấy dữ liệu Catalog thời gian thực ở phía máy chủ; trường `products` do trình
duyệt gửi không được dùng làm nguồn giá hoặc tồn kho của DynamicMart.

DynamicMart hiện đã dùng hai luồng dữ liệu:

- Chính sách, FAQ và hướng dẫn ổn định: đọc từ `.md`, `.txt`, `.pdf` rồi lập chỉ mục vào Qdrant.
- Sản phẩm, giá khuyến mãi và trạng thái còn hàng: AI Assistant gọi public contract của Catalog
  Service ở thời điểm khách hỏi. AI Assistant không kết nối `catalog_db` và không lưu bản sao
  giá/tồn kho trong Qdrant.

```text
Frontend -> API Gateway -> AI Assistant -> Catalog Service -> catalog_db
                                      <- ProductSummary <-
```

Khi Admin tạo, sửa, công khai sản phẩm hoặc điều chỉnh giá/tồn kho, chatbot dùng dữ liệu mới ở
câu hỏi kế tiếp; không phải tạo file tài liệu hoặc re-index Qdrant. Snapshot sản phẩm Markdown
cũ `danh-muc-va-san-pham-chi-tiet.md` không được loader lập chỉ mục.

## Chạy local

1. Cài và chạy Ollama, tải `qwen2.5:0.5b` và `qwen3-embedding:0.6b`.
2. Cài dependency và chạy FastAPI theo `services/ai-assistant-service/README.md`.
3. Gateway dùng `AI_ASSISTANT_SERVICE_URL=http://127.0.0.1:8001` theo mặc định.
4. Chạy Gateway và frontend như quy trình hiện có.

Môi trường phát triển chạy Qdrant Server bằng `docker compose up -d qdrant` trong
thư mục service, mặc định tại `http://127.0.0.1:6333`. Production đặt
`QDRANT_URL`, `QDRANT_API_KEY` và `QDRANT_COLLECTION` để kết nối Qdrant
Server/Cloud. Mỗi point lưu `tenant` trong payload và mọi truy vấn đều áp dụng
filter tenant phía Qdrant. Chế độ Qdrant in-memory chỉ dùng cho unit test.

Không đưa secret, dữ liệu thanh toán hoặc đơn hàng cá nhân vào `knowledge/`. Dữ liệu cá nhân trong tương lai phải đi qua API đã xác thực và kiểm tra ownership, không đưa vào vector store dùng chung.
