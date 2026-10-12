# Ecommerce AI Assistant

FastAPI RAG service dùng chung cho `Fashion_Ecommerce` và `DynamicMart`. Pipeline bám theo sườn của video tham khảo:

```text
DirectoryDocumentLoader
→ RecursiveTextSplitter
→ Ollama embedding
→ Qdrant vector database
→ hybrid retriever (semantic + từ khóa tiếng Việt)
→ context + prompt
→ Ollama chat model
```

Retriever chuẩn hóa một số cách diễn đạt tiếng Việt tương đương như vận chuyển/giao hàng,
mã giảm giá/voucher, COD/thanh toán khi nhận hàng và wishlist/sản phẩm yêu thích. Kết quả
được đa dạng hóa theo nguồn, tối đa hai chunk từ cùng một file. Splitter giữ nguyên đoạn/câu
ở biên chunk để citation không bắt đầu giữa từ. Chế độ trả lời nhanh có thể ghép tối đa ba
ý liên quan từ nhiều nguồn thay vì chỉ trả một câu đơn lẻ.

Để giảm độ trễ, câu hỏi tìm sản phẩm đi thẳng đến Catalog Service và FAQ đơn giản trả lời
trích xuất từ đoạn RAG phù hợp nhất. Khi bật LLM synthesis, chỉ câu hỏi cần so sánh, tổng hợp
hoặc tư vấn mới gọi chat model. Kết nối HTTP đến Catalog/Ollama được tái sử dụng, embedding câu hỏi lặp lại được
cache giới hạn. Embedding model được giữ nóng bằng `OLLAMA_EMBEDDING_KEEP_ALIVE`; chat model
mặc định giải phóng sau khi trả lời để tránh hai model chiếm GPU lâu và làm lỗi lượt hỏi sau.

Mỗi thư mục con trong `knowledge/` là một `tenant`. Dữ liệu của hai dự án không được truy xuất chéo.

Riêng DynamicMart, câu hỏi tìm sản phẩm được đọc trực tiếp từ Catalog Service qua HTTP.
Giá, giá khuyến mãi và trạng thái còn hàng không được sao chép vào Qdrant. Khi quản trị viên
cập nhật Catalog, câu hỏi tiếp theo dùng dữ liệu mới mà không cần re-index.

Môi trường local chạy Qdrant Server thật bằng Docker Compose và lưu dữ liệu trong named volume.
Khi deploy, thay `QDRANT_URL` và `QDRANT_API_KEY` để dùng Qdrant Server hoặc Qdrant Cloud;
contract API và pipeline không thay đổi. Qdrant in-memory chỉ được dùng trong unit test.

## Cài đặt

Yêu cầu Python 3.12 và Ollama. Từ thư mục service:

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
ollama pull qwen3:1.7b
ollama pull qwen3-embedding:0.6b
docker compose up -d qdrant
.venv\Scripts\python run.py
```

Không commit file `.env`. Có thể sao chép `.env.example` thành `.env` cho môi trường local, nhưng tiến trình Python không tự đọc `.env`; hãy nạp biến môi trường bằng công cụ chạy của nhóm hoặc dùng giá trị mặc định.

Qdrant local mặc định:

```powershell
docker compose up -d qdrant
Invoke-RestMethod http://127.0.0.1:6333
```

Catalog DynamicMart local mặc định ở `http://127.0.0.1:8082/api/v1/catalog` và storefront ở
`http://localhost:3000`. Khi deploy, cấu hình `DYNAMICMART_CATALOG_URL`,
`DYNAMICMART_STOREFRONT_URL` và `CATALOG_TIMEOUT_SECONDS` theo mạng nội bộ/môi trường thực tế.

Không expose cổng Qdrant ra mạng công cộng. Khi dùng Qdrant Cloud, đặt `QDRANT_URL`,
`QDRANT_API_KEY` và dùng một `QDRANT_COLLECTION` riêng cho embedding model hiện tại.

Nếu Ollama chưa chạy và `RAG_ALLOW_HASH_FALLBACK=true`, service dùng embedding băm xác định để test pipeline. Khi đó phần sinh câu trả lời dùng trích đoạn tốt nhất thay vì gọi LLM.

## API

- `GET /health`
- `POST /v1/chat` cho Laravel
- `POST /api/v1/assistant/chat` cho DynamicMart Gateway
- `POST /api/v1/assistant/chat/stream` trả NDJSON cho giao diện DynamicMart; các event gồm
  `token`, `replace`, `result` và `done`. Endpoint JSON cũ vẫn giữ nguyên contract.

Ví dụ:

```json
{
  "tenant": "dynamicmart",
  "message": "DynamicMart hỗ trợ thanh toán nào?",
  "products": [],
  "history": [
    {"role": "user", "content": "Tìm điện thoại dưới 20 triệu"},
    {"role": "assistant", "content": "Sản phẩm đã hiển thị: Dynamic Phone Pro."}
  ]
}
```

`history` là tùy chọn, tối đa 10 lượt, giúp hiểu câu hỏi nối tiếp như “mẫu đó còn hàng
không?”. Frontend chỉ giữ lịch sử trong phiên trang hiện tại và gửi kèm từng request; FastAPI
không lưu hội thoại vào database hay Qdrant. Nội dung có dấu hiệu chứa mật khẩu, OTP, token,
CVV hoặc số thẻ bị lược bỏ trước khi đưa vào truy vấn/prompt.

Các biến tối ưu Ollama mặc định là `OLLAMA_KEEP_ALIVE=0`,
`OLLAMA_EMBEDDING_KEEP_ALIVE=30m`, `OLLAMA_NUM_CTX=2048` và
`OLLAMA_NUM_PREDICT=160`. Có thể tăng giới hạn nếu cần câu trả lời dài hơn, đổi lại thời gian
và VRAM sẽ tăng. Retriever mặc định lấy 5 chunk, kích thước 700 ký tự và overlap 100 ký tự.
Embedding mặc định là `qwen3-embedding:0.6b`; collection mặc định mới
`ecommerce_knowledge_qwen3_06b` tách biệt với vector cũ để tránh trộn vector khác kích thước.
Chat model mặc định là `qwen3:1.7b` và `RAG_ENABLE_LLM_SYNTHESIS=true`. FAQ và câu hỏi nối tiếp
vẫn dùng retrieval theo history cùng câu trả lời trích xuất nhanh; câu hỏi yêu cầu giải thích,
so sánh hoặc tư vấn mới gọi chat model để diễn đạt rõ hơn. Chế độ thinking bị tắt để giảm độ trễ; mọi câu trả lời sinh vẫn được
kiểm tra số liệu và quy tắc grounding, nếu không đạt sẽ quay về câu trả lời trích xuất.

Response giữ contract chung:

```json
{
  "reply": "...",
  "intent": "knowledge_question",
  "query": "...",
  "products": [],
  "citations": [
    {"source": "dynamicmart/thanh-toan-va-giao-hang.md", "title": "...", "excerpt": "...", "score": 0.5}
  ]
}
```

## Thêm dữ liệu

Đặt file `.md`, `.txt` hoặc `.pdf` vào `knowledge/<tenant>/` rồi khởi động lại service. Startup sẽ nạp tài liệu, chia chunk, tạo embedding và thay chỉ mục Qdrant của từng tenant.

Kho DynamicMart hiện bao phủ mua hàng/checkout, trạng thái đơn, thanh toán–giao hàng,
voucher, tài khoản–địa chỉ, đánh giá, wishlist, hỏi đáp sản phẩm, thông báo và hỗ trợ khách hàng.
Khi bổ sung tri thức, ưu tiên cập nhật file đúng chủ đề thay vì tạo nội dung trùng lặp.

Không đưa mật khẩu, token, dữ liệu thanh toán hoặc dữ liệu đơn hàng cá nhân vào knowledge base chung.

Không xuất danh sách sản phẩm, giá hoặc tồn kho thành Markdown để làm tri thức RAG. File snapshot
`danh-muc-va-san-pham-chi-tiet.md` cũ bị loader bỏ qua nhằm tránh chatbot trả dữ liệu lỗi thời.
Sản phẩm mới chỉ cần được tạo và công khai trong Catalog Service; không cần khởi động lại FastAPI.

## Test

```powershell
python -m unittest discover -s tests -v
```
