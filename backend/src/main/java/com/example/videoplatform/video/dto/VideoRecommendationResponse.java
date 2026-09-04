package com.example.videoplatform.video.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record VideoRecommendationResponse(
        List<VideoRecommendationItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static VideoRecommendationResponse from(Page<VideoRecommendationItemResponse> result) {
        return new VideoRecommendationResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }
}
