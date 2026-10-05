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

Mỗi thư mục con trong `knowledge/` là một `tenant`. Dữ liệu của hai dự án không được truy xuất chéo.

Môi trường local chạy Qdrant Server thật bằng Docker Compose và lưu dữ liệu trong named volume.
Khi deploy, thay `QDRANT_URL` và `QDRANT_API_KEY` để dùng Qdrant Server hoặc Qdrant Cloud;
contract API và pipeline không thay đổi. Qdrant in-memory chỉ được dùng trong unit test.

## Cài đặt

Yêu cầu Python 3.12 và Ollama. Từ thư mục service:

```powershell
python -m venv .venv
.venv\Scripts\python -m pip install -r requirements.txt
ollama pull qwen2.5:0.5b
ollama pull nomic-embed-text
docker compose up -d qdrant
.venv\Scripts\python run.py
```

Không commit file `.env`. Có thể sao chép `.env.example` thành `.env` cho môi trường local, nhưng tiến trình Python không tự đọc `.env`; hãy nạp biến môi trường bằng công cụ chạy của nhóm hoặc dùng giá trị mặc định.

Qdrant local mặc định:

```powershell
docker compose up -d qdrant
Invoke-RestMethod http://127.0.0.1:6333
```

Không expose cổng Qdrant ra mạng công cộng. Khi dùng Qdrant Cloud, đặt `QDRANT_URL`,
`QDRANT_API_KEY` và dùng một `QDRANT_COLLECTION` riêng cho embedding model hiện tại.

Nếu Ollama chưa chạy và `RAG_ALLOW_HASH_FALLBACK=true`, service dùng embedding băm xác định để test pipeline. Khi đó phần sinh câu trả lời dùng trích đoạn tốt nhất thay vì gọi LLM.

## API

- `GET /health`
- `POST /v1/chat` cho Laravel
- `POST /api/v1/assistant/chat` cho DynamicMart Gateway

Ví dụ:

```json
{
  "tenant": "dynamicmart",
  "message": "DynamicMart hỗ trợ thanh toán nào?",
  "products": []
}
```

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

Không đưa mật khẩu, token, dữ liệu thanh toán hoặc dữ liệu đơn hàng cá nhân vào knowledge base chung.

## Test

```powershell
python -m unittest discover -s tests -v
```
