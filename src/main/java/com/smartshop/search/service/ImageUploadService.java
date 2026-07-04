package com.smartshop.search.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Service for uploading images to AWS S3.
 * Handles multipart file uploads and generates S3 URLs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    private final S3Client s3Client;

    @Value("${app.s3.bucket:smartshop-search-bucket}")
    private String bucketName;

    @Value("${app.s3.region:us-east-1}")
    private String awsRegion;

    /**
     * Uploads an image file to AWS S3.
     * Generates a unique filename and returns the S3 URL.
     *
     * @param file multipart image file
     * @return S3 URL of the uploaded image
     * @throws IOException if file reading fails
     * @throws RuntimeException if S3 upload fails
     */
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);

        // Generate unique filename
        String s3Key = generateS3Key(fileExtension);

        try {
            log.debug("Uploading image to S3: {}", s3Key);

            // Upload to S3
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(file.getContentType())
                .build();

            PutObjectResponse response = s3Client.putObject(
                putObjectRequest,
                RequestBody.fromBytes(file.getBytes())
            );

            log.debug("Image uploaded successfully to S3");

            // Generate S3 URL
            String s3Url = generateS3Url(s3Key);
            log.info("Generated S3 URL: {}", s3Url);

            return s3Url;

        } catch (Exception e) {
            log.error("Error uploading image to S3: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to S3: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a unique S3 object key for the image.
     * Format: images/YYYY/MM/DD/{uuid}.{extension}
     *
     * @param fileExtension file extension
     * @return S3 object key
     */
    private String generateS3Key(String fileExtension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();
        return String.format("images/%s-%s.%s", timestamp, uuid, fileExtension);
    }

    /**
     * Generates a public S3 URL for the uploaded object.
     * Format: https://s3.{region}.amazonaws.com/{bucket}/{key}
     *
     * @param s3Key S3 object key
     * @return public S3 URL
     */
    private String generateS3Url(String s3Key) {
        return String.format("https://s3.%s.amazonaws.com/%s/%s", awsRegion, bucketName, s3Key);
    }

    /**
     * Extracts file extension from filename.
     *
     * @param filename the filename
     * @return file extension without the dot
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

}
