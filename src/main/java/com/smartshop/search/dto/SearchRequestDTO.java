package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for product search.
 * Contains query parameters for semantic similarity search.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequestDTO {

    /**
     * Query text to search for (required).
     */
    private String query;

    /**
     * Product category to filter by (optional).
     */
    private String category;

    /**
     * Maximum price filter (optional).
     */
    private Double maxPrice;

    /**
     * Maximum number of results to return (default: 10).
     */
    @Builder.Default
    private Integer limit = 10;

}
