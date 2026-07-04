package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO from CLIP image embedding endpoint.
 * Contains 512-dimensional image embedding vector.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageEmbeddingResponseDTO {

    /**
     * Image embedding vector as float array (512 dimensions for CLIP).
     */
    private float[] embedding;

    /**
     * Optional: model name or version used for embedding.
     */
    private String model;

}
