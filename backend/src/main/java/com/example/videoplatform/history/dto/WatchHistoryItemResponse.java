package com.example.videoplatform.history.dto;

import com.example.videoplatform.history.entity.WatchHistory;
import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record WatchHistoryItemResponse(
        Long videoId,
        String title,
        String thumbnailUrl,
        Long duration,
        Long positionSeconds,
        Integer progressPercent,
        WatchHistoryAuthorResponse author,
        LocalDateTime lastWatchedAt
) {
    public static WatchHistoryItemResponse from(WatchHistory history) {
        Video video = history.getVideo();
        long duration = video.getDurationSeconds() == null ? 0L : video.getDurationSeconds().longValue();
        long position = history.getPositionSeconds().longValue();
        int progress = duration == 0 ? 0 : (int) Math.min(100, position * 100 / duration);

        return new WatchHistoryItemResponse(
                video.getId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                duration,
                position,
                progress,
                WatchHistoryAuthorResponse.from(video.getUploader()),
                history.getLastWatchedAt()
        );
    }
}
