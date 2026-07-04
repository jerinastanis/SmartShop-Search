# SmartShop Search

A Spring Boot 3 RAG (Retrieval-Augmented Generation) search application using pgvector for semantic product search.

## Technology Stack

- **Java**: 17
- **Spring Boot**: 3.3.1
- **Spring Data JPA**: For ORM and database operations
- **PostgreSQL**: Primary database with pgvector extension
- **pgvector**: Vector similarity search capability
- **AWS S3**: (Optional) For storing product embeddings and artifacts
- **Lombok**: To reduce boilerplate code

## Project Structure

```
src/
├── main/
│   ├── java/com/smartshop/search/
│   │   ├── controller/     # REST API endpoints
│   │   ├── service/        # Business logic
│   │   ├── repository/     # Data access layer
│   │   ├── model/          # JPA entities
│   │   ├── dto/            # Data transfer objects
│   │   ├── config/         # Configuration classes
│   │   └── SmartShopSearchApplication.java  # Main application class
│   └── resources/
│       └── application.yml  # Application configuration
└── test/
    └── java/com/smartshop/search/  # Unit and integration tests
```

## API Endpoints

### Text Search
**POST /api/search**

```json
{
  "query": "wireless headphones",
  "category": "electronics",
  "maxPrice": 200.0,
  "limit": 10
}
```

### Image Search
**POST /api/search/image**

Form Data:
- `image` (file, required)
- `maxPrice` (optional)
- `limit` (optional)

```bash
curl -X POST http://localhost:8080/api/search/image \
  -F "image=@photo.jpg" \
  -F "maxPrice=150" \
  -F "limit=5"
```

### Search Comparison
**POST /api/search/compare**

Query Parameters:
- `query` (required)
- `limit` (optional, default: 10)

```bash
curl -X POST "http://localhost:8080/api/search/compare?query=headphones&limit=5"
```

---



### Prerequisites

- Java 17 or higher
- PostgreSQL 12+ with pgvector extension installed
- Gradle 7.0+ (or use ./gradlew)

### Installation

1. **Install pgvector extension in PostgreSQL**:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   ```

2. **Configure database connection** in `src/main/resources/application.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/smartshop_search
       username: your_username
       password: your_password
   ```

3. **Build the project**:
   ```bash
   mvn clean install
   ```

4. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

The application will start on `http://localhost:8080/api`

## Environment Variables

Configure the following environment variables or update `application.yml`:

- `DB_HOST`: PostgreSQL host (default: localhost)
- `DB_PORT`: PostgreSQL port (default: 5432)
- `DB_NAME`: Database name (default: smartshop_search)
- `DB_USER`: Database user (default: postgres)
- `DB_PASSWORD`: Database password (default: password)
- `SERVER_PORT`: Application port (default: 8080)
- `AWS_REGION`: AWS region (default: us-east-1)
- `AWS_S3_BUCKET`: S3 bucket name
- `AWS_ACCESS_KEY_ID`: AWS access key
- `AWS_SECRET_ACCESS_KEY`: AWS secret access key
- `OLLAMA_ENDPOINT_URL`: Ollama API endpoint (default: http://localhost:11434/api/generate)
- `OLLAMA_MODEL`: Ollama model to use (default: mistral)
- `OLLAMA_TIMEOUT`: Timeout for Ollama requests in seconds (default: 60)
- `DATA_LOADER_ON_STARTUP`: Enable automatic product loading on startup (default: false)
- `PRODUCTS_FILE`: Path to products JSON file (default: data/products.json)

## Next Steps

1. Implement entity models with vector embeddings
2. Develop repository methods for vector similarity search
3. Create service layer for search logic
4. Build REST API endpoints in controllers
5. Add integration with embedding models (OpenAI, Anthropic, etc.)
6. Implement AWS S3 integration for artifact storage
7. Add unit and integration tests
8. Set up Docker configuration for containerization

## Data Loading

The application includes a `DataLoaderService` that can automatically populate the database with products from a JSON file on startup.

### Configuration

Enable data loading by setting the following environment variables or in `application.yml`:

```yaml
data-loader:
  load-on-startup: true
  products-file: data/products.json
```

### JSON Schema

Product JSON file format (see `data/products.json` for example):

```json
[
  {
    "id": 1,
    "name": "Product Name",
    "description": "Product description with details",
    "category": "electronics",
    "price": 99.99,
    "imageUrl": "https://example.com/image.jpg"
  }
]
```

### Process

When enabled, DataLoaderService will:
1. Read products from the JSON file
2. Check if each product already exists in the database (by ID)
3. Skip existing products
4. Generate text embeddings for new products (name + description)
5. Save to database with embeddings
6. Log progress every 50 products

**Note**: Ensure the embedding service endpoint is running before enabling data loading.

## Match Explanations

The application includes a `MatchExplanationService` that generates LLM-powered explanations for why products match search queries.

### Configuration

Ensure you have Ollama running locally:

```bash
ollama pull mistral
ollama serve
```

Then configure in `application.yml`:

```yaml
ollama:
  endpoint-url: http://localhost:11434/api/generate
  model: mistral
  timeout-seconds: 60
```

### How It Works

When a search is performed:
1. Vector similarity search retrieves products
2. Price and category filters are applied
3. Match explanations are generated for the **top 5 results** to keep performance optimal
4. Each explanation explains in 1-2 sentences why that product matches the query

**Example Response**:
```json
{
  "results": [
    {
      "id": 1,
      "name": "Sony WH-CH720 Wireless Headphones",
      "description": "...",
      "category": "electronics",
      "price": 99.99,
      "imageUrl": "...",
      "similarityScore": 0.89,
      "matchExplanation": "These wireless headphones perfectly match your search for audio equipment with their 35-hour battery life and noise-canceling capabilities, making them ideal for long listening sessions."
    }
  ],
  "totalCount": 1,
  "executionTime": 2450
}
```

**Note**: If Ollama is unavailable, the search still works but returns `null` for explanations. Match explanations are optional and only generated for top results to optimize response time.

## Image Search

The application includes image-based semantic search using CLIP (Contrastive Language-Image Pre-training).

### Configuration

Ensure you have a CLIP embedding service running locally:

```bash
# Using a CLIP server (e.g., llm-clip-server or similar)
# Make sure it's running on http://localhost:8001
```

Then configure in `application.yml`:

```yaml
clip:
  endpoint-url: http://localhost:8001/embed-image
  timeout-seconds: 60

aws:
  region: us-east-1
  s3:
    bucket: smartshop-search-bucket
  access-key-id: ${AWS_ACCESS_KEY_ID}
  secret-access-key: ${AWS_SECRET_ACCESS_KEY}
```

### S3Config (AWS SDK v2)

The [S3Config](c:\Users\jeffr\Documents\RAG_SEARCH\SmartShop-Search\src\main\java\com\smartshop\search\config\S3Config.java) class provides a configured S3Client bean:

```java
@Configuration
public class S3Config {
    @Bean
    public S3Client s3Client() {
        // Creates S3 client with credentials from config
        // Supports both explicit credentials and default credential chain
    }
}
```

**Dependencies** (pom.xml):
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

### How Image Search Works

When uploading an image:
1. Image is sent to CLIP endpoint and converted to 512-dimensional embedding
2. pgvector similarity search runs against `imageEmbedding` column
3. Price filters are applied to results
4. Returns ProductDTO list same as text search

**Example Request**:
```bash
curl -X POST http://localhost:8080/api/search/image \
  -F "image=@product.jpg" \
  -F "maxPrice=150" \
  -F "limit=5"
```

**Response** (same as text search):
```json
{
  "results": [
    {
      "id": 1,
      "name": "Sony WH-CH720",
      "description": "...",
      "category": "electronics",
      "price": 99.99,
      "imageUrl": "https://s3.us-east-1.amazonaws.com/...",
      "similarityScore": 0.91,
      "matchExplanation": "This product visually matches your image search."
    }
  ],
  "totalCount": 1,
  "executionTime": 1250
}
```

### Services

- **ImageUploadService** - Handles multipart image upload to AWS S3, generates URLs
- **ImageEmbeddingService** - Calls CLIP endpoint, converts images to 512-dim embeddings
- **SearchService.searchByImage()** - Orchestrates image search with filtering and explanations

## Search Comparison

The application includes a comparison endpoint that runs keyword and semantic search methods in parallel for performance.

### Endpoint

**POST /api/search/compare**

Query Parameters:
- `query` (required) - Search query string
- `limit` (optional) - Result limit per search type (default: 10, max: 100)

### How It Works

Uses `CompletableFuture` to run both searches concurrently:

1. **Keyword Search** - PostgreSQL full-text search using tsvector/plainto_tsquery
   - Ranks by text relevance using `ts_rank()`
   - Fast for exact term matching
   - No similarity score

2. **Semantic Search** - pgvector embedding similarity search
   - Generates query embedding via embedding service
   - Ranks by cosine similarity
   - Captures semantic meaning

### Example Request

```bash
curl -X POST "http://localhost:8080/api/search/compare?query=wireless%20headphones&limit=5"
```

### Example Response

```json
{
  "keywordResults": [
    {
      "id": 1,
      "name": "Sony WH-CH720 Wireless Headphones",
      "description": "...",
      "category": "electronics",
      "price": 99.99,
      "imageUrl": "...",
      "similarityScore": null,
      "matchExplanation": null
    }
  ],
  "semanticResults": [
    {
      "id": 1,
      "name": "Sony WH-CH720 Wireless Headphones",
      "description": "...",
      "category": "electronics",
      "price": 99.99,
      "imageUrl": "...",
      "similarityScore": 0.87,
      "matchExplanation": null
    },
    {
      "id": 2,
      "name": "Apple AirPods Pro",
      "description": "...",
      "category": "electronics",
      "price": 249.00,
      "imageUrl": "...",
      "similarityScore": 0.85,
      "matchExplanation": null
    }
  ],
  "executionTime": 450,
  "keywordSearchTime": null,
  "semanticSearchTime": null
}
```

### Use Cases

- **Compare results**: See how keyword and semantic matching differ
- **Debug rankings**: Understand why products rank differently in each method
- **Quality assessment**: Verify both search methods work well together
- **Research**: Analyze search algorithm differences

### Performance Notes

- Runs both searches in parallel for optimal performance
- Execution time is approximately the slower of the two searches
- Neither search blocks the other thanks to CompletableFuture

## Gradle Commands

- `mvn clean install` - Build the project
- `mvn test` - Run tests
- `mvn spring-boot:run` - Run the application
- `mvn clean` - Clean build artifacts
- `mvn dependency:tree` - Show dependency tree

**Note**: The old Gradle files (`build.gradle`, `settings.gradle`) are no longer needed and can be removed.

## Configuration Management

The application uses Spring Boot profiles to manage environment-specific configuration. Three profiles are provided:

### Profile Structure

#### 1. **application.yml** (Base/Shared)
Contains common configuration shared across all profiles. Sets active profile to `dev` by default.

```yaml
spring:
  profiles:
    active: dev
  jpa:
    show-sql: false
    # ...
app:
  search:
    explanation-limit: 5  # Configurable number of results to explain
```

#### 2. **application-dev.yml** (Development)
Local development environment with hardcoded endpoints and credentials.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/smartshop_search
    username: postgres
    password: postgres

app:
  embedding:
    text-endpoint: http://localhost:8000/embed
    image-endpoint: http://localhost:8001/embed-image
  ollama:
    endpoint: http://localhost:11434/api/generate
  s3:
    bucket: smartshop-search-dev
    region: us-east-1
```

**Usage**: Default profile for local development

#### 3. **application-prod.yml** (Production)
Production environment with environment variable placeholders. **All secrets and endpoints use placeholders.**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

app:
  embedding:
    text-endpoint: ${EMBEDDING_TEXT_ENDPOINT}
    image-endpoint: ${EMBEDDING_IMAGE_ENDPOINT}
  ollama:
    endpoint: ${OLLAMA_ENDPOINT}
  s3:
    bucket: ${AWS_S3_BUCKET}
    region: ${AWS_REGION}
    access-key-id: ${AWS_ACCESS_KEY_ID}
    secret-access-key: ${AWS_SECRET_ACCESS_KEY}
```

**Usage**: Set `SPRING_PROFILES_ACTIVE=prod` and provide all required environment variables

### Switching Profiles

```bash
# Development (default)
mvn spring-boot:run

# Production
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run

# Or in Docker
java -Dspring.profiles.active=prod -jar app.jar
```

### Environment Variables (Production)

**Database**:
- `DB_HOST` - PostgreSQL host
- `DB_PORT` - PostgreSQL port
- `DB_NAME` - Database name
- `DB_USER` - Database user
- `DB_PASSWORD` - Database password

**Embedding Services**:
- `EMBEDDING_TEXT_ENDPOINT` - Sentence-transformers endpoint
- `EMBEDDING_IMAGE_ENDPOINT` - CLIP embedding endpoint

**LLM Services**:
- `OLLAMA_ENDPOINT` - Ollama API endpoint
- `OLLAMA_MODEL` - Ollama model name (default: mistral)

**AWS S3**:
- `AWS_S3_BUCKET` - S3 bucket name
- `AWS_REGION` - AWS region
- `AWS_ACCESS_KEY_ID` - AWS access key
- `AWS_SECRET_ACCESS_KEY` - AWS secret key

### .gitignore Rules

**Committed** (safe to commit):
- `application.yml` - Base configuration
- `application-dev.yml` - Development configuration
- `application-prod.yml` - Production template (no secrets)

**Ignored** (never commit):
- `application-local.yml` - Local overrides
- `application-*.local.yml` - Local environment files
- `.env` - Environment files
- `.env.local` - Local environment files

Ensure all profile files are version-controlled except local overrides.

## License

MIT
