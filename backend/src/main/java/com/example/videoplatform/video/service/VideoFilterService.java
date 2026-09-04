package com.example.videoplatform.video.service;

import com.example.videoplatform.category.repository.CategoryRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.video.dto.VideoSearchItemResponse;
import com.example.videoplatform.video.dto.VideoSearchResponse;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoFilterService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;

    public VideoFilterService(VideoRepository videoRepository, CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public VideoSearchResponse filter(String keywordValue, String categoryIdValue, String sortValue,
                                      String pageValue, String sizeValue) {
        String keyword = normalizeKeyword(keywordValue);
        Long categoryId = parseCategoryId(categoryIdValue);
        int page = parseInteger(pageValue, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        int size = parseInteger(sizeValue, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);
        Sort sort = parseSort(sortValue);

        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return VideoSearchResponse.from(videoRepository.filter(
                keyword == null ? null : toLikePattern(keyword),
                categoryId,
                VideoVisibility.PUBLIC,
                VideoStatus.PUBLISHED,
                PageRequest.of(page, size, sort)
        ).map(VideoSearchItemResponse::from));
    }

    private String normalizeKeyword(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long parseCategoryId(String value) {
        if (value == null) {
            return null;
        }
        try {
            long categoryId = Long.parseLong(value);
            if (categoryId < 1) {
                throw new BusinessException(ErrorCode.INVALID_CATEGORY_ID);
            }
            return categoryId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_CATEGORY_ID);
        }
    }

    private Sort parseSort(String value) {
        String normalized = value == null ? "LATEST" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LATEST" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case "OLDEST" -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case "POPULAR" -> Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"));
            default -> throw new BusinessException(ErrorCode.INVALID_VIDEO_SORT);
        };
    }

    private int parseInteger(String value, int defaultValue, ErrorCode errorCode) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(errorCode);
        }
    }

    private String toLikePattern(String keyword) {
        String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
