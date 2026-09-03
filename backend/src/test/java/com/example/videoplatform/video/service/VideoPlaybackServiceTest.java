package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
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
class VideoPlaybackServiceTest {

    @Mock
    private VideoRepository videoRepository;

    private VideoPlaybackService videoPlaybackService;

    @BeforeEach
    void setUp() {
        videoPlaybackService = new VideoPlaybackService(videoRepository);
    }

    @Test
    void returnsPlaybackInformationForPublishedVideo() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, 27L,
                "https://cdn.example.com/videos/152/master.m3u8");
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        var response = videoPlaybackService.getPlayback(99L, "152");

        assertThat(response.videoId()).isEqualTo(152L);
        assertThat(response.title()).isEqualTo("스프링 부트 입문 강의");
        assertThat(response.playbackUrl()).isEqualTo("https://cdn.example.com/videos/152/master.m3u8");
        assertThat(response.mediaType()).isEqualTo("application/x-mpegURL");
        assertThat(response.duration()).isEqualTo(634L);
        assertThat(response.expiresAt()).isNull();
    }

    @Test
    void allowsUploaderToPlayPrivateVideo() {
        Video video = video(VideoVisibility.PRIVATE, VideoStatus.PUBLISHED, 27L, "video.m3u8");
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertThat(videoPlaybackService.getPlayback(27L, "152").videoId()).isEqualTo(152L);
    }

    @Test
    void rejectsPrivateVideoForOtherUser() {
        Video video = video(VideoVisibility.PRIVATE, VideoStatus.PUBLISHED, 27L, "video.m3u8");
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> videoPlaybackService.getPlayback(99L, "152"), ErrorCode.VIDEO_ACCESS_DENIED);
    }

    @Test
    void rejectsVideoThatIsStillProcessing() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PROCESSING, 27L, null);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> videoPlaybackService.getPlayback(99L, "152"), ErrorCode.VIDEO_NOT_READY);
    }

    @Test
    void rejectsPublishedVideoWithoutPlaybackUrl() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, 27L, null);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> videoPlaybackService.getPlayback(99L, "152"), ErrorCode.VIDEO_NOT_READY);
    }

    @Test
    void rejectsInvalidVideoIdBeforeRepositoryLookup() {
        assertBusinessError(() -> videoPlaybackService.getPlayback(99L, "video"), ErrorCode.INVALID_VIDEO_ID);
        assertBusinessError(() -> videoPlaybackService.getPlayback(99L, "0"), ErrorCode.INVALID_VIDEO_ID);
        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(anyLong());
    }

    @Test
    void rejectsMissingVideo() {
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.empty());

        assertBusinessError(() -> videoPlaybackService.getPlayback(99L, "152"), ErrorCode.VIDEO_NOT_FOUND);
    }

    private void assertBusinessError(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private Video video(
            VideoVisibility visibility,
            VideoStatus status,
            Long uploaderId,
            String videoUrl
    ) {
        Video video = mock(Video.class);
        User uploader = mock(User.class);

        lenient().when(video.getId()).thenReturn(152L);
        lenient().when(video.getTitle()).thenReturn("스프링 부트 입문 강의");
        lenient().when(video.getDurationSeconds()).thenReturn(634);
        lenient().when(video.getVisibility()).thenReturn(visibility);
        lenient().when(video.getStatus()).thenReturn(status);
        lenient().when(video.getVideoUrl()).thenReturn(videoUrl);
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getId()).thenReturn(uploaderId);
        return video;
    }
}
