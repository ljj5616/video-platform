package com.example.videoplatform.video.service;

import com.example.videoplatform.category.repository.CategoryRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.dto.VideoUploadResponse;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import com.example.videoplatform.video.storage.VideoStorage;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoUploadService {
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final VideoStorage videoStorage;
    private final VideoProcessingService videoProcessingService;
    private final long maxVideoSize;
    private final long maxThumbnailSize;
    private final String defaultThumbnailUrl;

    public VideoUploadService(
            VideoRepository videoRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            VideoStorage videoStorage,
            VideoProcessingService videoProcessingService,
            @Value("${video.upload.max-video-size}") DataSize maxVideoSize,
            @Value("${video.upload.max-thumbnail-size}") DataSize maxThumbnailSize,
            @Value("${video.upload.default-thumbnail-url:}") String defaultThumbnailUrl
    ) {
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.videoStorage = videoStorage;
        this.videoProcessingService = videoProcessingService;
        this.maxVideoSize = maxVideoSize.toBytes();
        this.maxThumbnailSize = maxThumbnailSize.toBytes();
        this.defaultThumbnailUrl = defaultThumbnailUrl;
    }

    public VideoUploadResponse upload(Long userId, MultipartFile videoFile, MultipartFile thumbnailFile,
                                      String title, String description, Long categoryId, String visibilityValue) {
        validateVideo(videoFile);
        validateThumbnail(thumbnailFile);
        String normalizedTitle = validateTitle(title);
        String normalizedDescription = validateDescription(description);
        VideoVisibility visibility = parseVisibility(visibilityValue);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        var category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        String uploadId = UUID.randomUUID().toString();
        String originalKey = "videos/original/" + userId + "/" + uploadId + "." + extension(videoFile);
        String thumbnailKey = null;
        try {
            String originalUrl = upload(originalKey, videoFile);
            String thumbnailUrl = blankToNull(defaultThumbnailUrl);
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                thumbnailKey = "videos/thumbnails/" + userId + "/" + uploadId + "." + extension(thumbnailFile);
                thumbnailUrl = upload(thumbnailKey, thumbnailFile);
            }

            Video video = videoRepository.save(new Video(user, category, normalizedTitle,
                    normalizedDescription, visibility, originalUrl, thumbnailUrl));
            videoProcessingService.process(video.getId(), originalKey, userId + "/" + uploadId);
            return VideoUploadResponse.from(video);
        } catch (RuntimeException exception) {
            videoStorage.delete(originalKey);
            if (thumbnailKey != null) videoStorage.delete(thumbnailKey);
            throw exception;
        }
    }

    private String upload(String key, MultipartFile file) {
        try (var inputStream = file.getInputStream()) {
            return videoStorage.upload(key, inputStream, file.getSize(), file.getContentType());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    private void validateVideo(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        if (file.getSize() > maxVideoSize || !VIDEO_TYPES.contains(file.getContentType())
                || !VIDEO_EXTENSIONS.contains(extension(file))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_VIDEO_FORMAT);
        }
    }

    private void validateThumbnail(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > maxThumbnailSize || !IMAGE_TYPES.contains(file.getContentType())
                || !IMAGE_EXTENSIONS.contains(extension(file))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_THUMBNAIL_FORMAT);
        }
    }

    private String validateTitle(String title) {
        if (title == null) throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        String normalized = title.trim();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_TITLE);
        }
        return normalized;
    }

    private String validateDescription(String description) {
        if (description == null || description.isBlank()) return null;
        String normalized = description.trim();
        if (normalized.length() > 5_000) throw new BusinessException(ErrorCode.INVALID_VIDEO_DESCRIPTION);
        return normalized;
    }

    private VideoVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        try {
            return VideoVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_VISIBILITY);
        }
    }

    private String extension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
