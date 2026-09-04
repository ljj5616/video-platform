package com.example.videoplatform.history.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record WatchHistoryResponse(
        List<WatchHistoryItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static WatchHistoryResponse from(Page<WatchHistoryItemResponse> result) {
        return new WatchHistoryResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
