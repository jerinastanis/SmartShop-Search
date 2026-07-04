package com.smartshop.search.repository;

import com.smartshop.search.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query(value = """
        SELECT *, 1 - (text_embedding <=> CAST(:embedding AS vector)) AS similarity_score
        FROM products
        WHERE (:category IS NULL OR category = :category)
        AND (:maxPrice IS NULL OR price <= :maxPrice)
        AND text_embedding IS NOT NULL
        AND 1 - (text_embedding <=> CAST(:embedding AS vector)) > :threshold
        ORDER BY text_embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<Product> findByTextEmbeddingSimilarity(
            @Param("embedding") String embedding,
            @Param("category") String category,
            @Param("maxPrice") Double maxPrice,
            @Param("limit") int limit,
            @Param("threshold") double threshold
    );

    @Query(value = """
            SELECT * FROM products
            WHERE image_embedding IS NOT NULL
            ORDER BY image_embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> findByImageEmbeddingSimilarity(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT * FROM products
            WHERE to_tsvector('english', name || ' ' || COALESCE(description, ''))
            @@ plainto_tsquery('english', :query)
            ORDER BY ts_rank(
                to_tsvector('english', name || ' ' || COALESCE(description, '')),
                plainto_tsquery('english', :query)
            ) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Product> findByKeywordSearch(
            @Param("query") String query,
            @Param("limit") int limit
    );
}