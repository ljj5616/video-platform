package com.example.videoplatform.video.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.video.dto.VideoDetailResponse;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoDetailService {

    private final VideoRepository videoRepository;

    public VideoDetailService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public VideoDetailResponse getDetail(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        Video video = videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        if (!canView(userId, video)) {
            throw new BusinessException(ErrorCode.VIDEO_ACCESS_DENIED);
        }
        return VideoDetailResponse.from(video);
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

    private boolean canView(Long userId, Video video) {
        boolean isUploader = userId != null && userId.equals(video.getUploader().getId());
        if (isUploader) {
            return true;
        }
        return video.getStatus() == VideoStatus.PUBLISHED
                && video.getVisibility() != VideoVisibility.PRIVATE;
    }
}
