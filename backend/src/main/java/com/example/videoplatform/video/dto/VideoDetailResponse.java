package com.example.videoplatform.video.dto;

import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record VideoDetailResponse(
        Long videoId,
        String title,
        String description,
        String thumbnailUrl,
        Long duration,
        Long viewCount,
        Long categoryId,
        String categoryName,
        VideoAuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static VideoDetailResponse from(Video video) {
        return new VideoDetailResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getDurationSeconds() == null ? null : video.getDurationSeconds().longValue(),
                video.getViewCount(),
                video.getCategory().getId(),
                video.getCategory().getName(),
                VideoAuthorResponse.from(video.getUploader()),
                video.getCreatedAt(),
                video.getUpdatedAt()
        );
    }
}
