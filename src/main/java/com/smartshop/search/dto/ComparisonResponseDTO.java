package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for search comparison.
 * Contains results from both keyword and semantic search methods.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComparisonResponseDTO {

    /**
     * Results from PostgreSQL full-text search (keyword-based).
     */
    private List<ProductDTO> keywordResults;

    /**
     * Results from pgvector semantic search (embedding-based).
     */
    private List<ProductDTO> semanticResults;

    /**
     * Total execution time in milliseconds for both searches.
     */
    private Long executionTime;

    /**
     * Time taken for keyword search in milliseconds.
     */
    private Long keywordSearchTime;

    /**
     * Time taken for semantic search in milliseconds.
     */
    private Long semanticSearchTime;

}
