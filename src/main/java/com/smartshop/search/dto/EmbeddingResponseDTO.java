package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for embedding response from sentence-transformers service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingResponseDTO {

    /**
     * Embedding vector as float array.
     */
    private float[] embedding;

    /**
     * Optional: dimension of the embedding vector.
     */
    private Integer dimension;

    /**
     * Optional: model name or version used for embedding.
     */
    private String model;

}
