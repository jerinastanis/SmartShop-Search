import io

import numpy as np
import torch
from fastapi import FastAPI, File, UploadFile, HTTPException
from pydantic import BaseModel
from PIL import Image
from transformers import CLIPModel, CLIPProcessor

app = FastAPI(title="CLIP Embedding Service", version="1.0.0")


class TextRequest(BaseModel):
    text: str


MODEL_NAME = "openai/clip-vit-base-patch32"

# Load once at startup.
device = "cuda" if torch.cuda.is_available() else "cpu"
model = CLIPModel.from_pretrained(MODEL_NAME).to(device)
processor = CLIPProcessor.from_pretrained(MODEL_NAME)
model.eval()


def _normalize(vec: np.ndarray) -> np.ndarray:
    vec = vec.astype(np.float32)
    norm = np.linalg.norm(vec)
    if norm == 0:
        return vec
    return vec / norm


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "device": device}


@app.post("/embed-image")
def embed_image(file: UploadFile = File(...)) -> dict:
    if not file.filename:
        raise HTTPException(status_code=400, detail="No file provided")

    try:
        image_bytes = file.file.read()
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception as exc:
        raise HTTPException(status_code=400, detail=f"Invalid image: {exc}") from exc

    with torch.no_grad():
        inputs = processor(images=image, return_tensors="pt", padding=True)
        inputs = {k: v.to(device) for k, v in inputs.items()}
        image_features = model.get_image_features(**inputs)
        image_features = image_features.cpu().numpy()[0]

    embedding = _normalize(image_features).tolist()
    return {"embedding": embedding}


@app.post("/embed-text-clip")
def embed_text_clip(payload: TextRequest) -> dict:
    if not payload.text or not payload.text.strip():
        raise HTTPException(status_code=400, detail="Text must not be empty")

    with torch.no_grad():
        inputs = processor(text=[payload.text], return_tensors="pt", padding=True)
        inputs = {k: v.to(device) for k, v in inputs.items()}
        text_features = model.get_text_features(**inputs)
        text_features = text_features.cpu().numpy()[0]

    embedding = _normalize(text_features).tolist()
    return {"embedding": embedding}
