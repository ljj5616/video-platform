package com.example.videoplatform.video.dto;

import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record VideoUpdateResponse(
        Long videoId,
        String title,
        String description,
        String thumbnailUrl,
        Long categoryId,
        String categoryName,
        String visibility,
        LocalDateTime updatedAt
) {
    public static VideoUpdateResponse from(Video video) {
        return new VideoUpdateResponse(
                video.getId(),
                video.getTitle(),
                video.getDescription(),
                video.getThumbnailUrl(),
                video.getCategory().getId(),
                video.getCategory().getName(),
                video.getVisibility().name(),
                video.getUpdatedAt()
        );
    }
}
