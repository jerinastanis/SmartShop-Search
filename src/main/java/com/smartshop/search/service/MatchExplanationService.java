package com.smartshop.search.service;

import com.smartshop.search.dto.OllamaRequestDTO;
import com.smartshop.search.dto.OllamaResponseDTO;
import com.smartshop.search.dto.ProductDTO;
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
 * Service for generating match explanations using local Ollama LLM.
 * Explains why a product matches a search query using semantic understanding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchExplanationService {

    private final RestTemplate restTemplate;

    @Value("${app.ollama.endpoint:http://localhost:11434/api/generate}")
    private String ollamaEndpointUrl;

    @Value("${app.ollama.model:mistral}")
    private String ollamaModel;

    /**
     * Generates a concise explanation of why a product matches the search query.
     * Calls the local Ollama LLM to provide semantic matching rationale.
     *
     * @param query the original search query
     * @param product the product to explain
     * @return explanation string (1-2 sentences)
     * @throws RuntimeException if the Ollama service is unavailable
     */
    public String generateExplanation(String query, ProductDTO product) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("Query is empty, skipping explanation generation");
            return null;
        }

        if (product == null || product.getName() == null) {
            log.warn("Product or product name is null, skipping explanation");
            return null;
        }

        try {
            log.debug("Generating explanation for product: {} with query: {}", product.getId(), query);

            String explanationMode = product != null && product.getExplanationMode() != null
                    ? product.getExplanationMode()
                    : "text";

            String prompt = buildPrompt(query, product, explanationMode);

            // Call Ollama API
            OllamaResponseDTO response = callOllama(prompt);

            if (response == null || response.getResponse() == null) {
                log.warn("Ollama returned null response");
                return null;
            }

            String explanation = response.getResponse().trim();
            log.debug("Generated explanation: {}", explanation);

            return explanation;

        } catch (Exception e) {
            log.error("Error generating explanation for product {}: {}", product.getId(), e.getMessage(), e);
            return null; // Return null if explanation fails, don't break the search
        }
    }

    /**
     * Calls the Ollama API to generate text.
     *
     * @param prompt the prompt to send to the model
     * @return Ollama response with generated text
     * @throws RestClientException if API call fails
     */
    private OllamaResponseDTO callOllama(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            OllamaRequestDTO request = OllamaRequestDTO.builder()
                .model(ollamaModel)
                .prompt(prompt)
                .stream(false)
                .build();

            HttpEntity<OllamaRequestDTO> httpEntity = new HttpEntity<>(request, headers);

            OllamaResponseDTO response = restTemplate.postForObject(
                ollamaEndpointUrl,
                httpEntity,
                OllamaResponseDTO.class
            );

            return response;

        } catch (RestClientException e) {
            log.error("Error calling Ollama API at {}: {}", ollamaEndpointUrl, e.getMessage(), e);
            throw new RuntimeException("Failed to call Ollama service: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the prompt for the LLM explaining product match.
     * Instructs the model to provide a brief, specific explanation.
     *
     * @param query the search query
     * @param product the product being explained
     * @return formatted prompt string
     */
    private String buildPrompt(String query, ProductDTO product, String explanationMode) {
        String productName = product.getName();
        String description = product.getDescription() != null ? product.getDescription() : "";
        String category = product.getCategory() != null ? product.getCategory() : "";
        String similarity = product.getSimilarityScore() != null
                ? String.format("%.2f", product.getSimilarityScore())
                : "unknown";

        if ("image".equalsIgnoreCase(explanationMode)) {
            return String.format(
                "Explain in 1-2 sentences why the product '%s' visually matches an image query. " +
                "Focus on visual similarity: color, shape, category, style, and overall appearance. " +
                "Product name: %s. Category: %s. Description: %s. Similarity score: %s. Be specific and concise.",
                productName,
                productName,
                category,
                description,
                similarity
            );
        }

        if ("multimodal".equalsIgnoreCase(explanationMode)) {
            return String.format(
                "Explain in 1-2 sentences why the product '%s' matches a text-to-image search query. " +
                "This is a multimodal CLIP-style match, so focus on visual similarity that aligns with the text prompt: color, shape, category, style, and appearance. " +
                "Product name: %s. Category: %s. Description: %s. Similarity score: %s. Be specific and concise.",
                productName,
                productName,
                category,
                description,
                similarity
            );
        }

        return String.format(
            "Explain in 1-2 sentences why the product '%s' matches the text search query '%s'. " +
            "Base the explanation on semantic relevance, product description, and category fit. " +
            "Product description: %s. Be specific and concise.",
            productName,
            query,
            description
        );
    }

}
