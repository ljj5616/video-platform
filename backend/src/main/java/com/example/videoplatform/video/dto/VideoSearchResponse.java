package com.example.videoplatform.video.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record VideoSearchResponse(
        List<VideoSearchItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static VideoSearchResponse from(Page<VideoSearchItemResponse> result) {
        return new VideoSearchResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }
}
