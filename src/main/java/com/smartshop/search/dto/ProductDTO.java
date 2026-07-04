package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object for Product in search results.
 * Includes similarity score and match explanation for ranking and context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    /**
     * Product unique identifier.
     */
    private Long id;

    /**
     * Product name.
     */
    private String name;

    /**
     * Product description.
     */
    private String description;

    /**
     * Product category.
     */
    private String category;

    /**
     * Product price.
     */
    private BigDecimal price;

    /**
     * Product image URL.
     */
    private String imageUrl;

    /**
     * Cosine similarity score between query embedding and product text embedding.
     * Range: -1.0 to 1.0 (higher is more similar).
     */
    private Double similarityScore;

    /**
     * LLM-generated explanation of why this product matches the search query.
     * Only populated for top results to keep performance optimal.
     */
    private String matchExplanation;

    /**
     * Optional hint describing which search mode produced this result.
     * Used by the explanation service to choose the right prompt style.
     */
    private String explanationMode;

}
