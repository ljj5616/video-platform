package com.example.videoplatform.video.service;

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
public class VideoSearchService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final VideoRepository videoRepository;

    public VideoSearchService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public VideoSearchResponse search(String keyword, String pageValue, String sizeValue) {
        String normalizedKeyword = normalizeKeyword(keyword);
        int page = parsePage(pageValue);
        int size = parseSize(sizeValue);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        return VideoSearchResponse.from(videoRepository.search(
                toLikePattern(normalizedKeyword),
                VideoVisibility.PUBLIC,
                VideoStatus.PUBLISHED,
                pageable
        ).map(VideoSearchItemResponse::from));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private int parsePage(String value) {
        int page = parseInteger(value, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        return page;
    }

    private int parseSize(String value) {
        int size = parseInteger(value, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
        return size;
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
        String escaped = keyword
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
