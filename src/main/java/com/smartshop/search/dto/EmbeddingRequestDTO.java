package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for embedding request to sentence-transformers service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmbeddingRequestDTO {

    /**
     * Text to be embedded.
     */
    private String text;

}
