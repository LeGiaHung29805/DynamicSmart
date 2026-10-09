from __future__ import annotations

import json
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.responses import StreamingResponse

from app.config import Settings
from app.rag.service import RagService
from app.schemas import ChatRequest, ChatResponse, HealthResponse


settings = Settings.from_environment()
rag_service = RagService(settings)


@asynccontextmanager
async def lifespan(_: FastAPI):
    await rag_service.ingest()
    try:
        yield
    finally:
        await rag_service.close()


from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="Ecommerce AI Assistant",
    version="0.1.0",
    description="RAG chatbot dùng chung cho Fashion Ecommerce và DynamicMart.",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        embedding_provider=rag_service.embedding_provider.name,
        vector_store="qdrant",
        vector_store_mode=rag_service.store.mode,
        tenants=rag_service.store.counts_by_tenant(),
    )


@app.post("/v1/chat", response_model=ChatResponse)
async def chat(request: ChatRequest) -> ChatResponse:
    return await rag_service.ask(request)


@app.post("/api/v1/assistant/chat", response_model=ChatResponse, include_in_schema=False)
async def dynamicmart_chat(request: ChatRequest) -> ChatResponse:
    return await rag_service.ask(request.model_copy(update={"tenant": "dynamicmart"}))


@app.post("/api/v1/assistant/chat/stream", include_in_schema=False)
async def dynamicmart_chat_stream(request: ChatRequest) -> StreamingResponse:
    dynamicmart_request = request.model_copy(update={"tenant": "dynamicmart"})

    async def events():
        async for event in rag_service.stream(dynamicmart_request):
            yield json.dumps(event, ensure_ascii=False) + "\n"

    return StreamingResponse(
        events(),
        media_type="application/x-ndjson",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )
