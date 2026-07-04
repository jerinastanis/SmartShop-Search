from typing import Any

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer

app = FastAPI()
model = SentenceTransformer('all-MiniLM-L6-v2')  # 384 dimensions, fast and small


class EmbeddingRequest(BaseModel):
    text: str


class EmbeddingResponse(BaseModel):
    embedding: list[float]


@app.post("/embed")
async def embed(request: Request):
    try:
        payload = await request.json()
    except Exception:
        payload = None

    text: Any = None
    if isinstance(payload, str):
        text = payload
    elif isinstance(payload, dict):
        text = payload.get('text')
        if text is None and isinstance(payload.get('request'), dict):
            text = payload['request'].get('text')
    elif payload is not None:
        text = getattr(payload, 'text', None)

    if not isinstance(text, str) or not text.strip():
        raise HTTPException(status_code=422, detail=[{"loc": ["body"], "msg": "Field required", "type": "missing"}])

    embedding = model.encode(text).tolist()
    return EmbeddingResponse(embedding=embedding)


@app.get("/health")
def health():
    return {"status": "ok"}