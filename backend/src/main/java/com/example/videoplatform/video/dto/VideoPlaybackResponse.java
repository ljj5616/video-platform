package com.example.videoplatform.video.dto;

import com.example.videoplatform.video.entity.Video;

public record VideoPlaybackResponse(
        Long videoId,
        String title,
        String playbackUrl,
        String mediaType,
        Long duration,
        String expiresAt
) {

    private static final String HLS_MEDIA_TYPE = "application/x-mpegURL";

    public static VideoPlaybackResponse from(Video video) {
        return new VideoPlaybackResponse(
                video.getId(),
                video.getTitle(),
                video.getVideoUrl(),
                HLS_MEDIA_TYPE,
                video.getDurationSeconds().longValue(),
                null
        );
    }
}
