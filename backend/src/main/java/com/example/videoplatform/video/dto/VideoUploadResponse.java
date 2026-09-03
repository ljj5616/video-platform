package com.example.videoplatform.video.dto;

import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record VideoUploadResponse(Long videoId, String status, LocalDateTime createdAt) {
    public static VideoUploadResponse from(Video video) {
        return new VideoUploadResponse(video.getId(), video.getStatus().name(), video.getCreatedAt());
    }
}
