package com.example.videoplatform.history.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.history.entity.WatchHistory;
import com.example.videoplatform.history.entity.WatchHistoryId;
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

@Service
@Transactional(readOnly = true)
public class WatchHistoryService {

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

    private boolean createHistory(Long userId, Video video, LocalDateTime watchedAt) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        watchHistoryRepository.save(new WatchHistory(user, video, watchedAt));
        return true;
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
