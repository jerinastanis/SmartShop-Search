package com.smartshop.search.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LocalImageStorageService implements ImageStorageService {

    @Value("${app.image-storage.local-root:data/product-images}")
    private String localRoot;

    @Override
    public Optional<Path> resolveImagePath(Long productId, String originalImageUrl) {
        if (productId != null) {
            Optional<Path> imagePath = resolveFromRoot(productId + ".jpg");
            if (imagePath.isPresent()) {
                return imagePath;
            }
        }

        if (originalImageUrl != null && !originalImageUrl.isBlank()) {
            String fileName = extractFileName(originalImageUrl);
            if (fileName != null) {
                Optional<Path> fallbackPath = resolveFromRoot(fileName);
                if (fallbackPath.isPresent()) {
                    return fallbackPath;
                }
            }
        }

        log.debug("No local image file found for product {} under {}", productId, localRoot);
        return Optional.empty();
    }

    private Optional<Path> resolveFromRoot(String fileName) {
        Path candidate = Paths.get(localRoot).resolve(fileName);
        if (isRegularFile(candidate)) {
            return Optional.of(candidate);
        }

        if (Paths.get(localRoot).isAbsolute()) {
            String relativeRoot = localRoot.replaceFirst("^[\\/]+", "");
            Path relativeCandidate = Paths.get(System.getProperty("user.dir")).resolve(relativeRoot).resolve(fileName);
            if (isRegularFile(relativeCandidate)) {
                return Optional.of(relativeCandidate);
            }
        }

        return Optional.empty();
    }

    private boolean isRegularFile(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    private String extractFileName(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.replace('\\', '/');
        int lastSlash = trimmed.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < trimmed.length() - 1) {
            return trimmed.substring(lastSlash + 1);
        }
        return trimmed;
    }
}
