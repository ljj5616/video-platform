package com.example.videoplatform.video.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VideoDeleteService {

    private final VideoRepository videoRepository;

    public VideoDeleteService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional
    public void delete(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        Video video = videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        if (userId == null || !userId.equals(video.getUploader().getId())) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }

        video.delete(LocalDateTime.now());
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
