package com.example.videoplatform.history.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.history.entity.WatchHistory;
import com.example.videoplatform.history.entity.WatchHistoryId;
import com.example.videoplatform.history.dto.WatchHistoryItemResponse;
import com.example.videoplatform.history.dto.WatchHistoryResponse;
import com.example.videoplatform.history.repository.WatchHistoryRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
@Transactional(readOnly = true)
public class WatchHistoryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public WatchHistoryService(WatchHistoryRepository watchHistoryRepository,
                               VideoRepository videoRepository,
                               UserRepository userRepository) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordView(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        Video video = videoRepository.findByIdAndDeletedAtIsNullForUpdate(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        if (!canView(userId, video)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_READY);
        }

        WatchHistoryId historyId = new WatchHistoryId(userId, videoId);
        LocalDateTime now = LocalDateTime.now();
        boolean shouldIncrease = watchHistoryRepository.findById(historyId)
                .map(history -> history.countView(now))
                .orElseGet(() -> createHistory(userId, video, now));

        if (shouldIncrease) {
            video.increaseViewCount();
        }
    }

    public WatchHistoryResponse getHistory(Long userId, String pageValue, String sizeValue) {
        int page = parsePage(pageValue);
        int size = parseSize(sizeValue);
        return WatchHistoryResponse.from(watchHistoryRepository
                .findByUser_IdOrderByLastWatchedAtDesc(userId, PageRequest.of(page, size))
                .map(WatchHistoryItemResponse::from));
    }

    @Transactional
    public void saveProgress(Long userId, String videoIdValue, Long positionSeconds) {
        long videoId = parseVideoId(videoIdValue);
        Video video = videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        if (!canView(userId, video)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        if (video.getStatus() != VideoStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.VIDEO_NOT_READY);
        }
        validatePosition(positionSeconds, video.getDurationSeconds());

        WatchHistoryId historyId = new WatchHistoryId(userId, videoId);
        WatchHistory history = watchHistoryRepository.findById(historyId)
                .orElseGet(() -> createProgressHistory(userId, video));
        history.updateProgress(positionSeconds.intValue(), LocalDateTime.now());
    }

    @Transactional
    public void deleteHistory(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        WatchHistoryId historyId = new WatchHistoryId(userId, videoId);
        if (watchHistoryRepository.existsById(historyId)) {
            watchHistoryRepository.deleteById(historyId);
        }
    }

    private boolean createHistory(Long userId, Video video, LocalDateTime watchedAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        watchHistoryRepository.save(new WatchHistory(user, video, watchedAt));
        return true;
    }

    private WatchHistory createProgressHistory(Long userId, Video video) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        WatchHistory history = new WatchHistory(user, video, LocalDateTime.now());
        watchHistoryRepository.save(history);
        return history;
    }

    private void validatePosition(Long position, Integer duration) {
        if (position == null || position < 0 || position > Integer.MAX_VALUE
                || duration == null || position > duration.longValue()) {
            throw new BusinessException(ErrorCode.INVALID_WATCH_POSITION);
        }
    }

    private int parsePage(String value) {
        int page = parseInteger(value, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        return page;
    }

    private int parseSize(String value) {
        int size = parseInteger(value, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);
        if (size < 1 || size > MAX_SIZE) throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        return size;
    }

    private int parseInteger(String value, int defaultValue, ErrorCode errorCode) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(errorCode);
        }
    }

    private boolean canView(Long userId, Video video) {
        return userId.equals(video.getUploader().getId())
                || video.getVisibility() != VideoVisibility.PRIVATE;
    }

    private long parseVideoId(String value) {
        try {
            long videoId = Long.parseLong(value);
            if (videoId < 1) {
                throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
            }
            return videoId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
        }
    }
}
