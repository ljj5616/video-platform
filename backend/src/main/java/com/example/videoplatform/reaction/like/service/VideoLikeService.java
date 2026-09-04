package com.example.videoplatform.reaction.like.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.reaction.like.entity.VideoLike;
import com.example.videoplatform.reaction.like.entity.VideoLikeId;
import com.example.videoplatform.reaction.like.repository.VideoLikeRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VideoLikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoLikeService(VideoLikeRepository videoLikeRepository, VideoRepository videoRepository,
                            UserRepository userRepository) {
        this.videoLikeRepository = videoLikeRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void like(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        Video video = findVideo(videoId);
        VideoLikeId likeId = new VideoLikeId(userId, videoId);
        if (videoLikeRepository.existsById(likeId)) {
            throw new BusinessException(ErrorCode.VIDEO_ALREADY_LIKED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        videoLikeRepository.save(new VideoLike(user, video));
        video.increaseLikeCount();
    }

    @Transactional
    public void unlike(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        Video video = findVideo(videoId);
        VideoLikeId likeId = new VideoLikeId(userId, videoId);
        VideoLike videoLike = videoLikeRepository.findById(likeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_LIKE_NOT_FOUND));

        videoLikeRepository.delete(videoLike);
        video.decreaseLikeCount();
    }

    private Video findVideo(long videoId) {
        return videoRepository.findByIdAndDeletedAtIsNullForUpdate(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }

    private long parseVideoId(String value) {
        try {
            long videoId = Long.parseLong(value);
            if (videoId < 1) throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
            return videoId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
        }
    }
}
