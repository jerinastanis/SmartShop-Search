package com.smartshop.search.service;

import java.nio.file.Path;
import java.util.Optional;

public interface ImageStorageService {

    Optional<Path> resolveImagePath(Long productId, String originalImageUrl);
}
