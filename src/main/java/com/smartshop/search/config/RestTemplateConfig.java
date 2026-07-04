package com.smartshop.search.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for RestTemplate bean.
 * Provides a configured RestTemplate for HTTP communication with external services.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a RestTemplate bean with timeout configurations.
     *
     * @param builder RestTemplateBuilder for fluent configuration
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(30))
            .build();
    }

}
