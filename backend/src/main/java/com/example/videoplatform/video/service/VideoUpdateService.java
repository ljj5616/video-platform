package com.example.videoplatform.video.service;

import com.example.videoplatform.category.entity.Category;
import com.example.videoplatform.category.repository.CategoryRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.video.dto.VideoUpdateResponse;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import com.example.videoplatform.video.storage.VideoStorage;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class VideoUpdateService {
    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;
    private final VideoStorage videoStorage;
    private final long maxThumbnailSize;

    public VideoUpdateService(VideoRepository videoRepository, CategoryRepository categoryRepository,
                              VideoStorage videoStorage,
                              @Value("${video.upload.max-thumbnail-size}") DataSize maxThumbnailSize) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
        this.videoStorage = videoStorage;
        this.maxThumbnailSize = maxThumbnailSize.toBytes();
    }

    @Transactional
    public VideoUpdateResponse update(Long userId, String videoIdValue, String title, String description,
                                      Long categoryId, MultipartFile thumbnailFile, String visibilityValue) {
        long videoId = parseVideoId(videoIdValue);
        validateAtLeastOneField(title, description, categoryId, thumbnailFile, visibilityValue);
        String normalizedTitle = title == null ? null : validateTitle(title);
        String normalizedDescription = description == null ? null : validateDescription(description);
        VideoVisibility visibility = visibilityValue == null ? null : parseVisibility(visibilityValue);
        validateThumbnail(thumbnailFile);

        Video video = videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        if (userId == null || !userId.equals(video.getUploader().getId())) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        if (video.getStatus() == VideoStatus.UPLOADING || video.getStatus() == VideoStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_EDITABLE);
        }

        Category category = null;
        if (categoryId != null) {
            if (categoryId < 1) throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        String thumbnailKey = null;
        try {
            String thumbnailUrl = null;
            if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
                thumbnailKey = "videos/thumbnails/" + userId + "/" + UUID.randomUUID() + "." + extension(thumbnailFile);
                thumbnailUrl = upload(thumbnailKey, thumbnailFile);
            }
            video.update(normalizedTitle, normalizedDescription, category, visibility, thumbnailUrl);
            if (description != null && description.isBlank()) video.clearDescription();
            videoRepository.flush();
            return VideoUpdateResponse.from(video);
        } catch (RuntimeException exception) {
            if (thumbnailKey != null) videoStorage.delete(thumbnailKey);
            throw exception;
        }
    }

    private void validateAtLeastOneField(String title, String description, Long categoryId,
                                         MultipartFile thumbnailFile, String visibility) {
        if (title == null && description == null && categoryId == null
                && (thumbnailFile == null || thumbnailFile.isEmpty()) && visibility == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }
    }

    private long parseVideoId(String value) {
        try {
            long videoId = Long.parseLong(value);
            if (videoId < 1) throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
            return videoId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
        }
    }

    private String validateTitle(String value) {
        String normalized = value.trim();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_TITLE);
        }
        return normalized;
    }

    private String validateDescription(String value) {
        String normalized = value.trim();
        if (normalized.length() > 5_000) throw new BusinessException(ErrorCode.INVALID_VIDEO_DESCRIPTION);
        return normalized;
    }

    private VideoVisibility parseVisibility(String value) {
        if (value.isBlank()) throw new BusinessException(ErrorCode.INVALID_VIDEO_VISIBILITY);
        try {
            return VideoVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_VISIBILITY);
        }
    }

    private void validateThumbnail(MultipartFile file) {
        if (file == null || file.isEmpty()) return;
        if (file.getSize() > maxThumbnailSize || !IMAGE_TYPES.contains(file.getContentType())
                || !IMAGE_EXTENSIONS.contains(extension(file))) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_THUMBNAIL_FORMAT);
        }
    }

    private String upload(String key, MultipartFile file) {
        try (var inputStream = file.getInputStream()) {
            return videoStorage.upload(key, inputStream, file.getSize(), file.getContentType());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.FILE_STORAGE_FAILED);
        }
    }

    private String extension(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
