package com.example.videoplatform.reaction.bookmark.dto;

import com.example.videoplatform.reaction.bookmark.entity.Bookmark;
import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record BookmarkItemResponse(
        Long videoId,
        String title,
        String thumbnailUrl,
        Long duration,
        Long viewCount,
        BookmarkAuthorResponse author,
        LocalDateTime bookmarkedAt
) {
    public static BookmarkItemResponse from(Bookmark bookmark) {
        Video video = bookmark.getVideo();
        long duration = video.getDurationSeconds() == null ? 0L : video.getDurationSeconds().longValue();
        return new BookmarkItemResponse(
                video.getId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                duration,
                video.getViewCount(),
                BookmarkAuthorResponse.from(video.getUploader()),
                bookmark.getCreatedAt()
        );
    }
}
