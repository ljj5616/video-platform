package com.example.videoplatform.reaction.like.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.reaction.like.entity.VideoLike;
import com.example.videoplatform.reaction.like.entity.VideoLikeId;
import com.example.videoplatform.reaction.like.repository.VideoLikeRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoLikeServiceTest {

    @Mock VideoLikeRepository videoLikeRepository;
    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;

    private VideoLikeService videoLikeService;

    @BeforeEach
    void setUp() {
        videoLikeService = new VideoLikeService(videoLikeRepository, videoRepository, userRepository);
    }

    @Test
    void likesVideo() {
        Video video = mock(Video.class);
        User user = mock(User.class);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));
        when(userRepository.findById(27L)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(27L);
        when(video.getId()).thenReturn(152L);

        videoLikeService.like(27L, "152");

        verify(videoLikeRepository).save(any(VideoLike.class));
        verify(video).increaseLikeCount();
    }

    @Test
    void rejectsDuplicateLike() {
        Video video = mock(Video.class);
        VideoLikeId id = new VideoLikeId(27L, 152L);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));
        when(videoLikeRepository.existsById(id)).thenReturn(true);

        assertBusinessError(() -> videoLikeService.like(27L, "152"), ErrorCode.VIDEO_ALREADY_LIKED);
        verify(videoLikeRepository, never()).save(any());
        verify(video, never()).increaseLikeCount();
    }

    @Test
    void unlikesVideo() {
        Video video = mock(Video.class);
        VideoLike like = mock(VideoLike.class);
        VideoLikeId id = new VideoLikeId(27L, 152L);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));
        when(videoLikeRepository.findById(id)).thenReturn(Optional.of(like));

        videoLikeService.unlike(27L, "152");

        verify(videoLikeRepository).delete(like);
        verify(video).decreaseLikeCount();
    }

    @Test
    void rejectsUnlikeWithoutExistingLike() {
        Video video = mock(Video.class);
        VideoLikeId id = new VideoLikeId(27L, 152L);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));
        when(videoLikeRepository.findById(id)).thenReturn(Optional.empty());

        assertBusinessError(() -> videoLikeService.unlike(27L, "152"), ErrorCode.VIDEO_LIKE_NOT_FOUND);
        verify(video, never()).decreaseLikeCount();
    }

    @Test
    void rejectsInvalidVideoIdBeforeRepositoryLookup() {
        assertBusinessError(() -> videoLikeService.like(27L, "video"), ErrorCode.INVALID_VIDEO_ID);
        assertBusinessError(() -> videoLikeService.unlike(27L, "0"), ErrorCode.INVALID_VIDEO_ID);
        verify(videoRepository, never()).findByIdAndDeletedAtIsNullForUpdate(any());
    }

    @Test
    void rejectsMissingVideo() {
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.empty());

        assertBusinessError(() -> videoLikeService.like(27L, "152"), ErrorCode.VIDEO_NOT_FOUND);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
