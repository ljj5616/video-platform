package com.example.videoplatform.video.service;

import com.example.videoplatform.category.repository.CategoryRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.video.dto.VideoSearchItemResponse;
import com.example.videoplatform.video.dto.VideoSearchResponse;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoCategoryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;

    public VideoCategoryService(VideoRepository videoRepository, CategoryRepository categoryRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public VideoSearchResponse getByCategory(String categoryIdValue, String pageValue, String sizeValue) {
        long categoryId = parseCategoryId(categoryIdValue);
        int page = parseInteger(pageValue, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        int size = parseInteger(sizeValue, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);

        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return VideoSearchResponse.from(videoRepository
                .findByCategory_IdAndVisibilityAndStatusAndDeletedAtIsNull(
                        categoryId, VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, pageable)
                .map(VideoSearchItemResponse::from));
    }

    private long parseCategoryId(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
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
}
