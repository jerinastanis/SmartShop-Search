# Python Embedding Services

This folder contains lightweight FastAPI services used by the SmartShop search project for text and image embeddings.

## Services

- `text_embedding_search.py` — text embeddings using Sentence Transformers
- `image_embedding_search.py` — image embeddings using CLIP

## Setup

```bash
cd python
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

## Run

Text embedding service:

```bash
python -m uvicorn text_embedding_search:app --host 0.0.0.0 --port 8000
```

Image embedding service:

```bash
python -m uvicorn image_embedding_search:app --host 0.0.0.0 --port 8001
```

## Notes

- The Java app expects the text service at `http://localhost:8000/embed`.
- The Java app expects the image service at `http://localhost:8001/embed-image`.
