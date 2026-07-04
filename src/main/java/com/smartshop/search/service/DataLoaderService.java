package com.smartshop.search.service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.search.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for loading product data from JSON file and populating the database.
 * Runs on application startup if data-loader.load-on-startup is enabled.
 * Generates embeddings for each product using EmbeddingService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataLoaderService {

    private final EmbeddingService embeddingService;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ImageStorageService imageStorageService;
    private final ImageEmbeddingService imageEmbeddingService;
    private final ApplicationArguments applicationArguments;

    @Value("${app.data.load-on-startup:false}")
    private boolean loadOnStartup;

    @Value("${app.data-loader.force:false}")
    private boolean forceOnStartup;

    @Value("${app.data-loader.products-file:data/products.json}")
    private String productsFilePath;

    /**
     * Loads product data from JSON file on application startup.
     * This method is called automatically after the bean is constructed.
     * Skips products that already exist in the database (by ID).
     * Logs progress every 50 products.
     */
    @jakarta.annotation.PostConstruct
    public void loadDataOnStartup() {
        if (!loadOnStartup) {
            log.debug("Data loader is disabled (load-on-startup: false)");
            return;
        }

        boolean forceRun = forceOnStartup || applicationArguments.containsOption("force");
        log.info("Starting data loader from file: {} (force={})", productsFilePath, forceRun);
        long startTime = System.currentTimeMillis();

        try {
            loadProductsFromJson(forceRun);
        } catch (Exception e) {
            log.error("Failed to load products from JSON file: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("Data loading completed in {} ms", duration);
    }

    /**
     * Reads product data from JSON file and saves to database.
     *
     * @throws Exception if file reading or processing fails
     */
    private void loadProductsFromJson(boolean forceRun) throws Exception {
        File file = new File(productsFilePath);

        if (!file.exists()) {
            log.warn("Products file not found: {}", productsFilePath);
            return;
        }

        // Read JSON array from file
        String fileContent = new String(Files.readAllBytes(Paths.get(productsFilePath)));
        ProductJsonWrapper[] productsArray = objectMapper.readValue(fileContent, ProductJsonWrapper[].class);

        log.info("Found {} products in JSON file", productsArray.length);

        int processedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (int i = 0; i < productsArray.length; i++) {
            ProductJsonWrapper jsonProduct = productsArray[i];
            String productLabel = jsonProduct.getName() != null ? jsonProduct.getName() : "product-" + (i + 1);

            try {
                // Check if product already exists
//                if (productRepository.existsById(jsonProduct.getId())) {
//                    log.debug("Product {} already exists in database, skipping", productLabel);
//                    skippedCount++;
//                    continue;
//                }

                Optional<com.smartshop.search.model.Product> existingProduct = Optional.empty();
                if (jsonProduct.getId() != null) {
                    existingProduct = productRepository.findById(jsonProduct.getId());
                }

                String textEmbeddingStr = null;
                String imageEmbeddingStr = null;
                boolean shouldUpdate = false;

                if (existingProduct.isPresent()) {
                    com.smartshop.search.model.Product existing = existingProduct.get();
                    boolean hasTextEmbedding = existing.getTextEmbedding() != null && !existing.getTextEmbedding().isBlank();
                    boolean hasImageEmbedding = existing.getImageEmbedding() != null && !existing.getImageEmbedding().isBlank();

                    if (!forceRun && hasTextEmbedding && hasImageEmbedding) {
                        log.debug("Skipping product {} because both embeddings already exist", productLabel);
                        skippedCount++;
                        continue;
                    }

                    if (forceRun || !hasTextEmbedding) {
                        String textToEmbed = jsonProduct.getName() + " " + jsonProduct.getDescription();
                        log.debug("Generating text embedding for product: {}", productLabel);
                        float[] textEmbedding = embeddingService.embedText(textToEmbed);
                        textEmbeddingStr = convertFloatArrayToString(textEmbedding);
                        shouldUpdate = true;
                    }

                    Path resolvedImagePath = imageStorageService.resolveImagePath(jsonProduct.getId(), jsonProduct.getImageUrl())
                            .orElse(null);
                    if (forceRun || !hasImageEmbedding) {
                        if (resolvedImagePath != null) {
                            log.debug("Found local image for product {} at {}", productLabel, resolvedImagePath);
                            try {
                                float[] imageEmbedding = imageEmbeddingService.embedImage(resolvedImagePath);
                                imageEmbeddingStr = convertFloatArrayToString(imageEmbedding);
                                shouldUpdate = true;
                            } catch (Exception imageEx) {
                                log.warn("Failed to generate image embedding for product {}: {}", productLabel, imageEx.getMessage());
                            }
                        } else {
                            log.warn("Missing local image for product {}, skipping image embedding", productLabel);
                        }
                    }

                    if (shouldUpdate) {
                        LocalDateTime now = LocalDateTime.now();
                        jdbcTemplate.update(
                                buildProductUpdateSql(),
                                textEmbeddingStr != null ? textEmbeddingStr : existing.getTextEmbedding(),
                                imageEmbeddingStr != null ? imageEmbeddingStr : existing.getImageEmbedding(),
                                Timestamp.valueOf(now),
                                existing.getId()
                        );
                        processedCount++;
                    } else {
                        skippedCount++;
                    }
                } else {
                    log.debug("Inserting new product {}", productLabel);
                    String textToEmbed = jsonProduct.getName() + " " + jsonProduct.getDescription();
                    float[] textEmbedding = embeddingService.embedText(textToEmbed);
                    textEmbeddingStr = convertFloatArrayToString(textEmbedding);

                    Path resolvedImagePath = imageStorageService.resolveImagePath(jsonProduct.getId(), jsonProduct.getImageUrl())
                            .orElse(null);
                    if (resolvedImagePath != null) {
                        try {
                            float[] imageEmbedding = imageEmbeddingService.embedImage(resolvedImagePath);
                            imageEmbeddingStr = convertFloatArrayToString(imageEmbedding);
                        } catch (Exception imageEx) {
                            log.warn("Failed to generate image embedding for product {}: {}", productLabel, imageEx.getMessage());
                        }
                    } else {
                        log.warn("Missing local image for product {}, inserting without image embedding", productLabel);
                    }

                    LocalDateTime now = LocalDateTime.now();
                    jdbcTemplate.update(
                        buildProductInsertSql(),
                        jsonProduct.getName(),
                        jsonProduct.getDescription(),
                        jsonProduct.getCategory(),
                        jsonProduct.getPrice(),
                        jsonProduct.getImageUrl(),
                        textEmbeddingStr,
                        imageEmbeddingStr,
                        Timestamp.valueOf(now),
                        Timestamp.valueOf(now)
                    );
                    processedCount++;
                }

                // Log progress every 50 products
                if (processedCount % 50 == 0) {
                    log.info("Progress: {} products loaded, {} skipped, {} errors",
                        processedCount, skippedCount, errorCount);
                }

            } catch (Exception e) {
                log.error("Error processing product {}: {}", jsonProduct.getId(), e.getMessage(), e);
                errorCount++;
            }
        }

        log.info("Data loading summary - Loaded: {}, Skipped: {}, Errors: {}, Total: {}",
            processedCount, skippedCount, errorCount, productsArray.length);
    }

    static String buildProductInsertSql() {
        return """
            INSERT INTO products (name, description, category, price, image_url, text_embedding, image_embedding, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, CAST(? AS vector(384)), CAST(? AS vector(512)), ?, ?)
            """;
    }

    static String buildProductUpdateSql() {
        return """
            UPDATE products
            SET text_embedding = CAST(? AS vector(384)),
                image_embedding = CAST(? AS vector(512)),
                updated_at = ?
            WHERE id = ?
            """;
    }

    /**
     * Converts float array to pgvector string representation.
     * Example: [0.1, 0.2, 0.3] -> "[0.1,0.2,0.3]"
     *
     * @param array float array
     * @return pgvector string format
     */
    private String convertFloatArrayToString(float[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Inner class for JSON deserialization of product data.
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProductJsonWrapper {
        private Long id;
        private String name;
        private String description;
        private String category;
        private BigDecimal price;
        private String imageUrl;
    }

}
