package com.smartshop.search.controller;

import com.smartshop.search.dto.ErrorResponseDTO;
import com.smartshop.search.dto.SearchRequestDTO;
import com.smartshop.search.dto.SearchResponseDTO;
import com.smartshop.search.dto.ComparisonResponseDTO;
import com.smartshop.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * REST Controller for search operations.
 * Handles search requests for products and catalog items.
 */
@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Performs semantic search on products.
     * Accepts search criteria and returns ranked results based on similarity.
     *
     * @param request search request containing query and optional filters
     * @return search response with products and metadata
     */
    @PostMapping
    public ResponseEntity<SearchResponseDTO> search(@RequestBody SearchRequestDTO request) {
        log.info("Received search request: {}", request.getQuery());

        try {
            SearchResponseDTO response = searchService.search(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing search request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Performs semantic search on products using image similarity (CLIP).
     * Accepts a multipart image file and returns ranked product results.
     *
     * @param imageFile the image file to search with (multipart)
     * @param maxPrice optional maximum price filter
     * @param limit optional result limit (default: 10, max: 100)
     * @return search response with products and metadata
     */
    @PostMapping("/image")
    public ResponseEntity<SearchResponseDTO> searchByImage(
        @RequestParam("image") MultipartFile imageFile,
        @RequestParam(value = "maxPrice", required = false) Double maxPrice,
        @RequestParam(value = "limit", required = false) Integer limit) {

        log.info("Received image search request");

        try {
            SearchResponseDTO response = searchService.searchByImage(imageFile, maxPrice, limit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image search request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("Error processing image file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing image search request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Performs image similarity search using CLIP embeddings from the local microservice.
     */
    @PostMapping("/by-image")
    public ResponseEntity<?> searchByImageEmbedding(
        @RequestParam("image") MultipartFile imageFile,
        @RequestParam(value = "limit", required = false) Integer limit) {

        log.info("Received by-image search request");

        try {
            SearchResponseDTO response = searchService.searchByImageEmbedding(imageFile, limit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid image embedding search request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                ErrorResponseDTO.builder()
                    .error("invalid_request")
                    .detail(e.getMessage())
                    .build());
        } catch (RuntimeException e) {
            log.error("CLIP service error during image embedding search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                ErrorResponseDTO.builder()
                    .error("clip_service_unavailable")
                    .detail("Unable to reach the CLIP embedding service at localhost:8001. Please ensure it is running.")
                    .build());
        } catch (IOException e) {
            log.error("Failed to read uploaded image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponseDTO.builder()
                    .error("invalid_image")
                    .detail("The uploaded image could not be read.")
                    .build());
        } catch (Exception e) {
            log.error("Unexpected error during image embedding search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponseDTO.builder()
                    .error("search_failed")
                    .detail("Image search failed unexpectedly.")
                    .build());
        }
    }

    /**
     * Distinct text-to-image search mode using CLIP.
     *
     * This is different from the original semantic text search because it uses the CLIP text encoder
     * on localhost:8001 and searches the products.image_embedding column, not the sentence-transformers
     * text embedding column used by the main text search.
     */
    @PostMapping("/by-text-to-image")
    public ResponseEntity<?> searchByTextToImage(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", required = false) Integer limit) {

        log.info("Received CLIP text-to-image search request");

        try {
            SearchResponseDTO response = searchService.searchByTextToImage(query, limit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid CLIP text-to-image request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    ErrorResponseDTO.builder()
                            .error("invalid_request")
                            .detail(e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            log.error("CLIP text-to-image search failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    ErrorResponseDTO.builder()
                            .error("clip_service_unavailable")
                            .detail("Unable to reach the CLIP embedding service at localhost:8001. Please ensure it is running.")
                            .build());
        } catch (Exception e) {
            log.error("Unexpected error during CLIP text-to-image search: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ErrorResponseDTO.builder()
                            .error("search_failed")
                            .detail("CLIP text-to-image search failed unexpectedly.")
                            .build());
        }
    }

    /**
     * Compares keyword search and semantic search results.
     * Runs both search methods in parallel and returns results from each approach.
     * Useful for understanding differences between keyword and semantic matching.
     *
     * @param query the search query
     * @param limit optional result limit per search type (default: 10, max: 100)
     * @return comparison response with both search results
     */
    @PostMapping("/compare")
    public ResponseEntity<ComparisonResponseDTO> compareSearchMethods(
        @RequestParam("query") String query,
        @RequestParam(value = "limit", required = false) Integer limit) {

        log.info("Received comparison search request: {}", query);

        try {
            ComparisonResponseDTO response = searchService.compareSearchMethods(query, limit);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid comparison request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing comparison request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
