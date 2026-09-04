package com.example.videoplatform.history.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

    @Mock WatchHistoryRepository watchHistoryRepository;
    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;

    private WatchHistoryService watchHistoryService;

    @BeforeEach
    void setUp() {
        watchHistoryService = new WatchHistoryService(
                watchHistoryRepository, videoRepository, userRepository);
    }

    @Test
    void recordsFirstViewAndIncreasesCount() {
        Video video = viewableVideo();
        User user = mock(User.class);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));
        when(watchHistoryRepository.findById(new WatchHistoryId(27L, 152L))).thenReturn(Optional.empty());
        when(userRepository.findById(27L)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(27L);

        watchHistoryService.recordView(27L, "152");

        verify(watchHistoryRepository).save(any(WatchHistory.class));
        verify(video).increaseViewCount();
    }

    @Test
    void doesNotIncreaseCountForAnAlreadyCountedView() {
        Video video = viewableVideo();
        WatchHistory history = mock(WatchHistory.class);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));
        when(watchHistoryRepository.findById(new WatchHistoryId(27L, 152L)))
                .thenReturn(Optional.of(history));
        when(history.countView(any())).thenReturn(false);

        watchHistoryService.recordView(27L, "152");

        verify(video, never()).increaseViewCount();
        verify(userRepository, never()).findById(any());
    }

    @Test
    void rejectsPrivateVideoForAnotherUser() {
        Video video = video(VideoVisibility.PRIVATE, VideoStatus.PUBLISHED, 99L);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> watchHistoryService.recordView(27L, "152"),
                ErrorCode.VIDEO_ACCESS_DENIED);
        verify(watchHistoryRepository, never()).findById(any());
    }

    @Test
    void rejectsVideoThatIsNotReady() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PROCESSING, 99L);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> watchHistoryService.recordView(27L, "152"),
                ErrorCode.VIDEO_NOT_READY);
    }

    @Test
    void rejectsInvalidAndMissingVideo() {
        assertBusinessError(() -> watchHistoryService.recordView(27L, "video"),
                ErrorCode.INVALID_VIDEO_ID);
        when(videoRepository.findByIdAndDeletedAtIsNullForUpdate(152L)).thenReturn(Optional.empty());
        assertBusinessError(() -> watchHistoryService.recordView(27L, "152"),
                ErrorCode.VIDEO_NOT_FOUND);
    }

    private Video viewableVideo() {
        return video(VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, 99L);
    }

    private Video video(VideoVisibility visibility, VideoStatus status, long uploaderId) {
        Video video = mock(Video.class);
        User uploader = mock(User.class);
        lenient().when(video.getId()).thenReturn(152L);
        lenient().when(video.getVisibility()).thenReturn(visibility);
        lenient().when(video.getStatus()).thenReturn(status);
        lenient().when(video.getUploader()).thenReturn(uploader);
        lenient().when(uploader.getId()).thenReturn(uploaderId);
        return video;
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
