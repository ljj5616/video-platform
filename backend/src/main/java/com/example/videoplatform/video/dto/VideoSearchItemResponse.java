package com.example.videoplatform.video.dto;

import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record VideoSearchItemResponse(
        Long videoId,
        String title,
        String thumbnailUrl,
        Long categoryId,
        String categoryName,
        String channelName,
        Long viewCount,
        LocalDateTime createdAt
) {
    public static VideoSearchItemResponse from(Video video) {
        return new VideoSearchItemResponse(
                video.getId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                video.getCategory().getId(),
                video.getCategory().getName(),
                video.getUploader().getNickname(),
                video.getViewCount(),
                video.getCreatedAt()
        );
    }
}
