package com.smartshop.search.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "app")
@Validated
@Data
public class AppProperties {

    private final Embedding embedding = new Embedding();
    private final Ollama ollama = new Ollama();
    private final S3 s3 = new S3();
    private final Search search = new Search();

    @Data
    public static class Embedding {
        @NotBlank
        private String textEndpoint;

        @NotBlank
        private String imageEndpoint;
    }

    @Data
    public static class Ollama {
        @NotBlank
        private String endpoint;
    }

    @Data
    public static class S3 {
        @NotBlank
        private String bucket;
    }

    @Data
    public static class Search {
        @Min(1)
        private int explanationLimit = 5;
    }
}
