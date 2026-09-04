package com.example.videoplatform.video.dto;

import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record VideoRecommendationItemResponse(
        Long id,
        String title,
        String thumbnailUrl,
        Long duration,
        Long viewCount,
        Long uploaderId,
        String uploaderNickname,
        LocalDateTime createdAt
) {
    public static VideoRecommendationItemResponse from(Video video) {
        return new VideoRecommendationItemResponse(
                video.getId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                video.getDurationSeconds() == null ? null : video.getDurationSeconds().longValue(),
                video.getViewCount(),
                video.getUploader().getId(),
                video.getUploader().getNickname(),
                video.getCreatedAt()
        );
    }
}
