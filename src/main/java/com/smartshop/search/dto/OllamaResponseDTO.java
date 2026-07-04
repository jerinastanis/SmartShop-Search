package com.smartshop.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Ollama API response with generated text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OllamaResponseDTO {

    /**
     * Generated text response from the model.
     */
    private String response;

    /**
     * Model name used for generation.
     */
    private String model;

    /**
     * Whether generation is complete.
     */
    private Boolean done;

    /**
     * Total duration in nanoseconds.
     */
    @JsonProperty("total_duration")
    private Long totalDuration;

}
