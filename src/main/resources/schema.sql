-- Enable pgvector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Create products table with vector embeddings
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    image_url VARCHAR(500),
    text_embedding VECTOR(384),
    image_embedding VECTOR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes on frequently queried columns
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_created_at ON products(created_at);

-- Create IVFFlat indexes for efficient vector similarity search
-- IVFFlat index for text embeddings (384 dimensions)
-- with 100 lists and 10 probes (tuned for 384-dim embeddings)
CREATE INDEX IF NOT EXISTS idx_products_text_embedding_ivfflat 
ON products USING ivfflat (text_embedding vector_cosine_ops)
WITH (lists = 100);

-- IVFFlat index for image embeddings (512 dimensions)
-- with 128 lists and 10 probes (tuned for 512-dim embeddings)
CREATE INDEX IF NOT EXISTS idx_products_image_embedding_ivfflat 
ON products USING ivfflat (image_embedding vector_cosine_ops)
WITH (lists = 128);

-- Create a view for easy vector similarity queries
CREATE OR REPLACE VIEW product_similarity_search AS
SELECT 
    id,
    name,
    description,
    category,
    price,
    image_url,
    text_embedding,
    image_embedding,
    created_at,
    updated_at
FROM products
WHERE text_embedding IS NOT NULL
   OR image_embedding IS NOT NULL;

-- Create composite index for category + text_embedding searches
CREATE INDEX IF NOT EXISTS idx_products_category_text_embedding 
ON products(category) 
INCLUDE (text_embedding);
