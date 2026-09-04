package com.example.videoplatform.reaction.like.dto;

import com.example.videoplatform.reaction.like.entity.VideoLike;
import com.example.videoplatform.video.entity.Video;
import java.time.LocalDateTime;

public record VideoLikeItemResponse(
        Long videoId,
        String title,
        String thumbnailUrl,
        Long duration,
        Long viewCount,
        Long likeCount,
        VideoLikeAuthorResponse author,
        LocalDateTime likedAt
) {
    public static VideoLikeItemResponse from(VideoLike videoLike) {
        Video video = videoLike.getVideo();
        long duration = video.getDurationSeconds() == null ? 0L : video.getDurationSeconds().longValue();
        return new VideoLikeItemResponse(
                video.getId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                duration,
                video.getViewCount(),
                video.getLikeCount(),
                VideoLikeAuthorResponse.from(video.getUploader()),
                videoLike.getCreatedAt()
        );
    }
}
