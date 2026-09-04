package com.example.videoplatform.comment.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record CommentPageResponse(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static CommentPageResponse from(Page<CommentResponse> result) {
        return new CommentPageResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
