package com.smartshop.search.service;

import com.smartshop.search.config.AppProperties;
import com.smartshop.search.dto.ProductDTO;
import org.springframework.web.multipart.MultipartFile;
import com.smartshop.search.dto.SearchRequestDTO;
import com.smartshop.search.dto.SearchResponseDTO;
import com.smartshop.search.dto.ComparisonResponseDTO;
import com.smartshop.search.model.Product;
import com.smartshop.search.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EmbeddingService embeddingService;
    private final ProductRepository productRepository;
    private final MatchExplanationService matchExplanationService;
    private final ImageEmbeddingService imageEmbeddingService;
    private final ClipTextEmbeddingService clipTextEmbeddingService;
    private final AppProperties appProperties;

    private int getExplanationLimit() {
        return appProperties.getSearch().getExplanationLimit();
    }

    public SearchResponseDTO search(SearchRequestDTO request) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Processing search request: query='{}', category='{}', maxPrice={}",
                    request.getQuery(), request.getCategory(), request.getMaxPrice());

            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                throw new IllegalArgumentException("Query cannot be null or empty");
            }

            int limit = request.getLimit() != null ? request.getLimit() : 10;
            if (limit <= 0 || limit > 100) {
                limit = 10;
            }

            log.debug("Generating embedding for query");
            float[] queryEmbedding = embeddingService.embedText(request.getQuery());

            log.debug("Performing vector similarity search with limit: {}", limit);
            String embeddingStr = convertFloatArrayToString(queryEmbedding);

            String category = request.getCategory() != null &&
                    !request.getCategory().trim().isEmpty()
                    ? request.getCategory() : null;

            List<Product> products = productRepository.findByTextEmbeddingSimilarity(
                    embeddingStr,
                    category,
                    request.getMaxPrice(),
                    limit,
                    0.3
            );

            log.debug("Retrieved {} products from database", products.size());

            List<ProductDTO> results = products.stream()
                    .map(product -> toProductDTO(product, queryEmbedding))
                    .collect(Collectors.toList());

            int explanationLimit = getExplanationLimit();
            log.debug("Generating match explanations for top {} results", explanationLimit);
            for (int i = 0; i < Math.min(results.size(), explanationLimit); i++) {
                ProductDTO product = results.get(i);
                String explanation = matchExplanationService.generateExplanation(
                        request.getQuery(), product);
                product.setMatchExplanation(explanation);
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Search completed in {} ms, returned {} results",
                    executionTime, results.size());

            return SearchResponseDTO.builder()
                    .results(results)
                    .totalCount(results.size())
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("Error processing search request: {}", e.getMessage(), e);
            throw new RuntimeException("Search operation failed: " + e.getMessage(), e);
        }
    }

    public SearchResponseDTO searchByImage(MultipartFile imageFile, Double maxPrice,
                                           Integer limit) throws IOException {
        long startTime = System.currentTimeMillis();

        try {
            if (imageFile == null || imageFile.isEmpty()) {
                throw new IllegalArgumentException("Image file cannot be null or empty");
            }

            int resultLimit = limit != null ? limit : 10;
            if (resultLimit <= 0 || resultLimit > 100) {
                resultLimit = 10;
            }

            log.info("Processing image search request with limit: {}", resultLimit);

            float[] imageEmbedding = imageEmbeddingService.embedImage(imageFile);
            String embeddingStr = convertFloatArrayToString(imageEmbedding);

            List<Product> products = productRepository.findByImageEmbeddingSimilarity(
                    embeddingStr,
                    resultLimit
            );

            log.debug("Retrieved {} products from database", products.size());

            List<ProductDTO> results = products.stream()
                    .map(product -> toProductDTOFromImage(product, imageEmbedding))
                    .collect(Collectors.toList());

            int explanationLimit = getExplanationLimit();
            for (int i = 0; i < Math.min(results.size(), explanationLimit); i++) {
                results.get(i).setMatchExplanation(
                        "This product visually matches your image search.");
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Image search completed in {} ms, returned {} results",
                    executionTime, results.size());

            return SearchResponseDTO.builder()
                    .results(results)
                    .totalCount(results.size())
                    .executionTime(executionTime)
                    .build();

        } catch (IOException e) {
            log.error("Error processing image file: {}", e.getMessage(), e);
            throw new IOException("Failed to process image file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error during image search: {}", e.getMessage(), e);
            throw new RuntimeException("Image search operation failed: " + e.getMessage(), e);
        }
    }

    public SearchResponseDTO searchByImageEmbedding(MultipartFile imageFile, Integer limit) throws IOException {
        long startTime = System.currentTimeMillis();

        try {
            validateImageFile(imageFile);

            int resultLimit = limit != null ? limit : 10;
            if (resultLimit <= 0 || resultLimit > 100) {
                resultLimit = 10;
            }

            log.info("Processing image embedding search request with limit: {}", resultLimit);

            float[] imageEmbedding = imageEmbeddingService.embedImage(imageFile);
            String embeddingStr = convertFloatArrayToString(imageEmbedding);

            List<Product> products = productRepository.findByImageEmbeddingSimilarity(
                    embeddingStr,
                    resultLimit
            );

            List<ProductDTO> results = products.stream()
                    .map(product -> toProductDTOFromImage(product, imageEmbedding))
                    .collect(Collectors.toList());

            int explanationLimit = getExplanationLimit();
            for (int i = 0; i < Math.min(results.size(), explanationLimit); i++) {
                results.get(i).setMatchExplanation(
                        "This product visually matches your image search.");
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Image embedding search completed in {} ms, returned {} results",
                    executionTime, results.size());

            return SearchResponseDTO.builder()
                    .results(results)
                    .totalCount(results.size())
                    .executionTime(executionTime)
                    .build();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error processing uploaded image file: {}", e.getMessage(), e);
            throw new IOException("Failed to process image file: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            log.error("CLIP image embedding search failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Distinct search mode: text query -> CLIP text embedding -> image_embedding similarity.
     *
     * This is different from the original semantic text search, which uses sentence-transformers
     * on localhost:8000 and searches the products.text_embedding column. This CLIP mode uses the
     * CLIP text encoder on localhost:8001 and searches products.image_embedding because CLIP
     * maps text and images into the same vector space.
     */
    public SearchResponseDTO searchByTextToImage(String query, Integer limit) {
        long startTime = System.currentTimeMillis();

        try {
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Query cannot be null or empty");
            }

            int resultLimit = limit != null ? limit : 10;
            if (resultLimit <= 0 || resultLimit > 100) {
                resultLimit = 10;
            }

            log.info("Processing CLIP text-to-image search request with limit: {}", resultLimit);

            float[] queryEmbedding = clipTextEmbeddingService.embedText(query);
            String embeddingStr = convertFloatArrayToString(queryEmbedding);

            List<Product> products = productRepository.findByImageEmbeddingSimilarity(
                    embeddingStr,
                    resultLimit
            );

            List<ProductDTO> results = products.stream()
                    .map(product -> toProductDTOFromImage(product, queryEmbedding, "multimodal"))
                    .collect(Collectors.toList());

            int explanationLimit = getExplanationLimit();
            for (int i = 0; i < Math.min(results.size(), explanationLimit); i++) {
                results.get(i).setMatchExplanation(
                        "This result matches your text query visually via CLIP similarity.");
            }

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("CLIP text-to-image search completed in {} ms, returned {} results",
                    executionTime, results.size());

            return SearchResponseDTO.builder()
                    .results(results)
                    .totalCount(results.size())
                    .executionTime(executionTime)
                    .build();

        } catch (RuntimeException e) {
            log.error("CLIP text-to-image search failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public ComparisonResponseDTO compareSearchMethods(String query, Integer limit) {
        long startTime = System.currentTimeMillis();

        try {
            if (query == null || query.trim().isEmpty()) {
                throw new IllegalArgumentException("Query cannot be null or empty");
            }

            int resultLimit = limit != null ? limit : 10;
            if (resultLimit <= 0 || resultLimit > 100) {
                resultLimit = 10;
            }

            final int finalLimit = resultLimit;

            log.info("Processing comparison search: query='{}', limit={}", query, finalLimit);

            CompletableFuture<List<ProductDTO>> keywordFuture =
                    CompletableFuture.supplyAsync(() ->
                            performKeywordSearch(query, finalLimit));

            CompletableFuture<List<ProductDTO>> semanticFuture =
                    CompletableFuture.supplyAsync(() ->
                            performSemanticSearch(query, finalLimit));

            List<ProductDTO> keywordResults = keywordFuture.join();
            List<ProductDTO> semanticResults = semanticFuture.join();

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Comparison search completed in {} ms", executionTime);

            return ComparisonResponseDTO.builder()
                    .keywordResults(keywordResults)
                    .semanticResults(semanticResults)
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("Error during comparison search: {}", e.getMessage(), e);
            throw new RuntimeException("Comparison search failed: " + e.getMessage(), e);
        }
    }

    private List<ProductDTO> performKeywordSearch(String query, int limit) {
        try {
            List<Product> products = productRepository.findByKeywordSearch(query, limit);
            return products.stream()
                    .map(product -> ProductDTO.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .description(product.getDescription())
                            .category(product.getCategory())
                            .price(product.getPrice())
                            .imageUrl(product.getImageUrl())
                            .similarityScore(null) // keyword search has no vector score
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error during keyword search: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<ProductDTO> performSemanticSearch(String query, int limit) {
        try {
            float[] queryEmbedding = embeddingService.embedText(query);
            String embeddingStr = convertFloatArrayToString(queryEmbedding);

            List<Product> products = productRepository.findByTextEmbeddingSimilarity(
                    embeddingStr,
                    null,
                    null,
                    limit,
                    0.3
            );

            return products.stream()
                    .map(product -> toProductDTO(product, queryEmbedding))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during semantic search: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // --- DTO mapping helpers ---

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/png") || contentType.equals("image/webp") || contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("Only JPEG, PNG, and WebP images are supported");
        }

        if (imageFile.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Image file size must be 5MB or less");
        }
    }

    private ProductDTO toProductDTO(Product product, float[] queryEmbedding) {
        Double score = computeSimilarityScore(queryEmbedding, product.getTextEmbedding());
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .similarityScore(score)
                .explanationMode("text")
                .build();
    }

    private ProductDTO toProductDTOFromImage(Product product, float[] queryImageEmbedding) {
        return toProductDTOFromImage(product, queryImageEmbedding, "image");
    }

    private ProductDTO toProductDTOFromImage(Product product, float[] queryImageEmbedding, String explanationMode) {
        Double score = computeSimilarityScore(queryImageEmbedding, product.getImageEmbedding());
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .similarityScore(score)
                .explanationMode(explanationMode)
                .build();
    }

    /**
     * Computes cosine similarity between a query embedding and a product's stored
     * embedding. If the product's embedding is stored as a String (e.g. pgvector
     * native query result like "[0.1,0.2,...]"), it's parsed first. If your Product
     * entity instead exposes a float[] directly, replace the parse call with the
     * direct getter and remove parseEmbeddingString.
     */
    private Double computeSimilarityScore(float[] queryEmbedding, String storedEmbeddingStr) {
        if (storedEmbeddingStr == null || storedEmbeddingStr.isBlank()) {
            return null;
        }
        float[] productEmbedding = parseEmbeddingString(storedEmbeddingStr);
        return calculateCosineSimilarity(queryEmbedding, productEmbedding);
    }

    private float[] parseEmbeddingString(String embeddingStr) {
        if (embeddingStr == null || embeddingStr.isBlank()) return new float[0];
        String cleaned = embeddingStr.replace("[", "").replace("]", "");
        if (cleaned.isBlank()) return new float[0];
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    private double calculateCosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0) {
            return 0.0;
        }
        int minLength = Math.min(a.length, b.length);
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < minLength; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double magnitude = Math.sqrt(normA) * Math.sqrt(normB);
        return magnitude == 0.0 ? 0.0 : dotProduct / magnitude;
    }

    private String convertFloatArrayToString(float[] array) {
        if (array == null || array.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}