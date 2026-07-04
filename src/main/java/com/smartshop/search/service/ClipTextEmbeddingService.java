package com.smartshop.search.service;

import com.smartshop.search.dto.ImageEmbeddingResponseDTO;
import com.smartshop.search.dto.TextImageEmbeddingRequestDTO;
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
 * CLIP-based text embedding service for text-to-image search.
 *
 * This is intentionally separate from the existing sentence-transformers text search flow:
 * - Original text search uses localhost:8000 and the sentence-transformers model against the products.text_embedding column.
 * - This CLIP mode uses localhost:8001/embed-text-clip and the CLIP text encoder against the products.image_embedding column.
 *
 * This works because CLIP maps text and images into the same vector space, so a text query can be compared
 * directly against image embeddings using cosine similarity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClipTextEmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${app.embedding.clip-text-endpoint:http://localhost:8001/embed-text-clip}")
    private String clipTextEndpointUrl;

    public float[] embedText(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            TextImageEmbeddingRequestDTO request = TextImageEmbeddingRequestDTO.builder()
                    .text(text)
                    .build();

            HttpEntity<TextImageEmbeddingRequestDTO> httpEntity = new HttpEntity<>(request, headers);

            ImageEmbeddingResponseDTO response = restTemplate.postForObject(
                    clipTextEndpointUrl,
                    httpEntity,
                    ImageEmbeddingResponseDTO.class
            );

            if (response == null || response.getEmbedding() == null) {
                throw new RuntimeException("CLIP text embedding service returned null response");
            }

            log.debug("Successfully generated CLIP text embedding with dimension: {}", response.getEmbedding().length);
            return response.getEmbedding();

        } catch (RestClientException e) {
            log.error("Error calling CLIP text embedding service at {}: {}", clipTextEndpointUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to generate CLIP text embedding: " + e.getMessage(), e);
        }
    }
}
