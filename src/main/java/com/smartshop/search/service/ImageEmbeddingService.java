package com.smartshop.search.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.smartshop.search.dto.ImageEmbeddingResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for generating image embeddings using a local CLIP HTTP endpoint.
 * Handles image-to-vector conversion for similarity search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageEmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${app.embedding.image-endpoint:http://localhost:8001/embed-image}")
    private String clipEndpointUrl;

    /**
     * Generates a 512-dimensional image embedding by calling the CLIP service.
     * Accepts a multipart image file and returns a float array.
     *
     * @param imageFile the multipart image file
     * @return float array representing the image embedding (512 dimensions)
     * @throws IOException if file reading fails
     * @throws RuntimeException if the CLIP service is unavailable
     */
    public float[] embedImage(MultipartFile imageFile) throws IOException {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty");
        }

        try {
            log.debug("Generating embedding for image: {}", imageFile.getOriginalFilename());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(imageFile.getBytes()) {
                @Override
                public String getFilename() {
                    return imageFile.getOriginalFilename() != null ? imageFile.getOriginalFilename() : "image.jpg";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, headers);

            ImageEmbeddingResponseDTO response = restTemplate.postForObject(
                clipEndpointUrl,
                httpEntity,
                ImageEmbeddingResponseDTO.class
            );

            if (response == null || response.getEmbedding() == null) {
                throw new RuntimeException("CLIP service returned null response");
            }

            log.debug("Successfully generated image embedding with dimension: {}", response.getEmbedding().length);
            return response.getEmbedding();

        } catch (RestClientException e) {
            log.error("Error calling CLIP service at {}: {}", clipEndpointUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to generate image embedding: " + e.getMessage(), e);
        }
    }

    public float[] embedImage(Path imagePath) throws IOException {
        if (imagePath == null || !Files.exists(imagePath) || !Files.isRegularFile(imagePath)) {
            throw new IllegalArgumentException("Image path is invalid: " + imagePath);
        }

        try {
            log.debug("Generating embedding for image path: {}", imagePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(Files.readAllBytes(imagePath)) {
                @Override
                public String getFilename() {
                    return imagePath.getFileName().toString();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(body, headers);

            ImageEmbeddingResponseDTO response = restTemplate.postForObject(
                clipEndpointUrl,
                httpEntity,
                ImageEmbeddingResponseDTO.class
            );

            if (response == null || response.getEmbedding() == null) {
                throw new RuntimeException("CLIP service returned null response");
            }

            log.debug("Successfully generated image embedding with dimension: {}", response.getEmbedding().length);
            return response.getEmbedding();

        } catch (RestClientException e) {
            log.error("Error calling CLIP service at {}: {}", clipEndpointUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to generate image embedding: " + e.getMessage(), e);
        }
    }

}
