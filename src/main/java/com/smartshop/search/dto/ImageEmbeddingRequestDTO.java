package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for CLIP image embedding endpoint.
 * Contains image bytes to embed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageEmbeddingRequestDTO {

    /**
     * Image file bytes in base64 or binary format.
     */
    private String image;

    /**
     * Image format (e.g., "png", "jpg").
     */
    private String format;

}
