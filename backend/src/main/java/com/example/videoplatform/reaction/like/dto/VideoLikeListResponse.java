package com.example.videoplatform.reaction.like.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record VideoLikeListResponse(
        List<VideoLikeItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static VideoLikeListResponse from(Page<VideoLikeItemResponse> result) {
        return new VideoLikeListResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
