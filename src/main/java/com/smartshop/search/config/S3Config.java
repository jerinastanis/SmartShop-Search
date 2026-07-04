package com.smartshop.search.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Configuration for AWS S3 client using SDK v2.
 * Provides S3Client bean for file upload and retrieval operations.
 */
@Configuration
public class S3Config {

    @Value("${app.s3.region:us-east-1}")
    private String awsRegion;

    @Value("${app.s3.access-key-id:}")
    private String accessKeyId;

    @Value("${app.s3.secret-access-key:}")
    private String secretAccessKey;

    /**
     * Creates and configures an S3Client bean.
     * Uses AWS SDK v2 with credentials from application properties or environment variables.
     *
     * @return configured S3Client instance
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
            .region(Region.of(awsRegion));

        // Use explicit credentials if provided, otherwise use default credential chain
        if (accessKeyId != null && !accessKeyId.isEmpty() &&
            secretAccessKey != null && !secretAccessKey.isEmpty()) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                )
            );
        }

        return builder.build();
    }

}
