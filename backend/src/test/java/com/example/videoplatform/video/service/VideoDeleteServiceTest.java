package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoDeleteServiceTest {

    @Mock
    private VideoRepository videoRepository;

    private VideoDeleteService videoDeleteService;

    @BeforeEach
    void setUp() {
        videoDeleteService = new VideoDeleteService(videoRepository);
    }

    @Test
    void deletesVideoUploadedByAuthenticatedUser() {
        Video video = mock(Video.class);
        User uploader = mock(User.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getId()).thenReturn(27L);

        videoDeleteService.delete(27L, "152");

        verify(video).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidVideoIdBeforeRepositoryLookup() {
        assertBusinessError(() -> videoDeleteService.delete(27L, "video"), ErrorCode.INVALID_VIDEO_ID);
        assertBusinessError(() -> videoDeleteService.delete(27L, "0"), ErrorCode.INVALID_VIDEO_ID);
        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsMissingOrAlreadyDeletedVideo() {
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.empty());

        assertBusinessError(() -> videoDeleteService.delete(27L, "152"), ErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    void rejectsUserWhoIsNotUploader() {
        Video video = mock(Video.class);
        User uploader = mock(User.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getId()).thenReturn(27L);

        assertBusinessError(() -> videoDeleteService.delete(99L, "152"), ErrorCode.VIDEO_ACCESS_DENIED);
        verify(video, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private void assertBusinessError(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}
