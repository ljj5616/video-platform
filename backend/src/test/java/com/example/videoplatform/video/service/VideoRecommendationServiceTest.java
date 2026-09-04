package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class VideoRecommendationServiceTest {

    @Mock VideoRepository videoRepository;
    private VideoRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new VideoRecommendationService(videoRepository);
    }

    @Test
    void getsRecommendationsWithDefaults() {
        Video video = video();
        when(videoRepository.findRecommendations(eq(27L), eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video), PageRequest.of(0, 20), 53));

        var response = service.getRecommendations(27L, null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(1L);
        assertThat(response.content().get(0).duration()).isEqualTo(615L);
        assertThat(response.content().get(0).uploaderId()).isEqualTo(10L);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(53);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findRecommendations(eq(27L), eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void rejectsInvalidPage() {
        assertBusinessError(() -> service.getRecommendations(27L, "-1", null),
                ErrorCode.INVALID_PAGE_NUMBER);
        assertBusinessError(() -> service.getRecommendations(27L, "first", null),
                ErrorCode.INVALID_PAGE_NUMBER);
        verify(videoRepository, never()).findRecommendations(any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidSize() {
        assertBusinessError(() -> service.getRecommendations(27L, null, "0"),
                ErrorCode.INVALID_PAGE_SIZE);
        assertBusinessError(() -> service.getRecommendations(27L, null, "101"),
                ErrorCode.INVALID_PAGE_SIZE);
        assertBusinessError(() -> service.getRecommendations(27L, null, "large"),
                ErrorCode.INVALID_PAGE_SIZE);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private Video video() {
        Video video = org.mockito.Mockito.mock(Video.class);
        User uploader = org.mockito.Mockito.mock(User.class);
        when(video.getId()).thenReturn(1L);
        when(video.getTitle()).thenReturn("제주도 여행 브이로그");
        when(video.getThumbnailUrl()).thenReturn("https://example.com/thumbnails/1.jpg");
        when(video.getDurationSeconds()).thenReturn(615);
        when(video.getViewCount()).thenReturn(12_500L);
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getId()).thenReturn(10L);
        when(uploader.getNickname()).thenReturn("여행자");
        when(video.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 19, 14, 30));
        return video;
    }
}
