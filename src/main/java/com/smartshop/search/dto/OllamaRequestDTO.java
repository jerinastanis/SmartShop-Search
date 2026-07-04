package com.smartshop.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Ollama API request to generate explanations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaRequestDTO {

    /**
     * Model name to use for generation.
     */
    private String model;

    /**
     * Prompt text to send to the model.
     */
    private String prompt;

    /**
     * Stream response flag (false for complete response).
     */
    @Builder.Default
    private Boolean stream = false;

}
