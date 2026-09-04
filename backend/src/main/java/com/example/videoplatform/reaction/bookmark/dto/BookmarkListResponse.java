package com.example.videoplatform.reaction.bookmark.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record BookmarkListResponse(
        List<BookmarkItemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static BookmarkListResponse from(Page<BookmarkItemResponse> result) {
        return new BookmarkListResponse(result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isFirst(), result.isLast());
    }
}
