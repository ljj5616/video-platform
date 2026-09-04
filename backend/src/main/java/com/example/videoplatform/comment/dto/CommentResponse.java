package com.example.videoplatform.comment.dto;

import com.example.videoplatform.comment.entity.Comment;
import java.time.LocalDateTime;

public record CommentResponse(
        Long commentId,
        Long videoId,
        String content,
        CommentAuthorResponse author,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getVideo().getId(),
                comment.getContent(),
                CommentAuthorResponse.from(comment.getUser()),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
