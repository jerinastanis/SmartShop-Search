package com.smartshop.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the SmartShop Search application.
 * Spring Boot 3 application with vector search capabilities using pgvector.
 */
@SpringBootApplication
public class SmartShopSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartShopSearchApplication.class, args);
    }

}
