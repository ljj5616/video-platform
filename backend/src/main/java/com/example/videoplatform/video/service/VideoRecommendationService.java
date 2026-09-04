package com.example.videoplatform.video.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.video.dto.VideoRecommendationItemResponse;
import com.example.videoplatform.video.dto.VideoRecommendationResponse;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoRecommendationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final VideoRepository videoRepository;

    public VideoRecommendationService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public VideoRecommendationResponse getRecommendations(Long userId, String pageValue, String sizeValue) {
        int page = parseInteger(pageValue, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        int size = parseInteger(sizeValue, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);
        if (page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        }

        return VideoRecommendationResponse.from(videoRepository.findRecommendations(
                userId,
                VideoVisibility.PUBLIC,
                VideoStatus.PUBLISHED,
                PageRequest.of(page, size)
        ).map(VideoRecommendationItemResponse::from));
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
