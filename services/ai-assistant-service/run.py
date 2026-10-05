from __future__ import annotations

import os

import uvicorn


if __name__ == "__main__":
    uvicorn.run(
        "app.main:app",
        host=os.getenv("AI_ASSISTANT_HOST", "127.0.0.1"),
        port=int(os.getenv("AI_ASSISTANT_PORT", "8001")),
        reload=False,
    )

