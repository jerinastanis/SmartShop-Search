package com.smartshop.search.service;

import com.smartshop.search.dto.EmbeddingRequestDTO;
import com.smartshop.search.dto.EmbeddingResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Service for generating embeddings using a local sentence-transformers HTTP endpoint.
 * Handles communication with the embedding model service and vector generation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${app.embedding.text-endpoint:http://localhost:8000/embed}")
    private String embeddingEndpointUrl;

    @Value("${app.embedding.text-max-length:512}")
    private int maxTextLength;

    /**
     * Generates a text embedding by calling the local sentence-transformers service.
     * The embedding is returned as a float array.
     *
     * @param text the text to embed
     * @return float array representing the text embedding
     * @throws RestClientException if the embedding service is unavailable or returns an error
     * @throws IllegalArgumentException if the text is null or empty
     */
    public float[] embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        // Truncate text if it exceeds max length
        String truncatedText = text.length() > maxTextLength 
            ? text.substring(0, maxTextLength) 
            : text;

        try {
            log.debug("Requesting embedding for text of length: {}", truncatedText.length());

            // Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            EmbeddingRequestDTO request = EmbeddingRequestDTO.builder()
                .text(truncatedText)
                .build();

            HttpEntity<EmbeddingRequestDTO> httpEntity = new HttpEntity<>(request, headers);

            // Call embedding service
            EmbeddingResponseDTO response = restTemplate.postForObject(
                embeddingEndpointUrl,
                httpEntity,
                EmbeddingResponseDTO.class
            );

            if (response == null || response.getEmbedding() == null) {
                throw new RuntimeException("Embedding service returned null response");
            }

            log.debug("Successfully generated embedding with dimension: {}", response.getEmbedding().length);
            return response.getEmbedding();

        } catch (RestClientException e) {
            log.error("Error calling embedding service at {}: {}", embeddingEndpointUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
        }
    }

    /**
     * Batch embed multiple texts.
     * Calls embedText for each text sequentially.
     *
     * @param texts array of texts to embed
     * @return array of embedding float arrays
     */
    public float[][] embedBatch(String[] texts) {
        if (texts == null || texts.length == 0) {
            throw new IllegalArgumentException("Texts array cannot be null or empty");
        }

        float[][] embeddings = new float[texts.length][];
        for (int i = 0; i < texts.length; i++) {
            log.debug("Processing batch item {}/{}", i + 1, texts.length);
            embeddings[i] = embedText(texts[i]);
        }
        return embeddings;
    }

}
