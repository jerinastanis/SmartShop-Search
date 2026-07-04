package com.smartshop.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for search responses.
 * Encapsulates search results and metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponseDTO {

    /**
     * List of products matching the search query.
     */
    private List<ProductDTO> results;

    /**
     * Total number of results returned.
     */
    private Integer totalCount;

    /**
     * Time taken to execute the search in milliseconds.
     */
    private Long executionTime;

}
