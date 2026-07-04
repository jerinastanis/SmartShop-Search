package com.smartshop.search.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataLoaderServiceTest {

    @Test
    void buildProductInsertSqlUsesVectorCastForTextEmbedding() {
        String sql = DataLoaderService.buildProductInsertSql();

        assertTrue(sql.contains("CAST(? AS vector(384))"));
        assertTrue(sql.contains("INSERT INTO products (name, description, category, price, image_url, text_embedding, image_embedding, created_at, updated_at)"));
    }
}
