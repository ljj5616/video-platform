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
class VideoSearchServiceTest {

    @Mock
    private VideoRepository videoRepository;

    private VideoSearchService videoSearchService;

    @BeforeEach
    void setUp() {
        videoSearchService = new VideoSearchService(videoRepository);
    }

    @Test
    void searchesPublishedPublicVideosWithDefaults() {
        Video video = video();
        PageRequest requestedPage = PageRequest.of(0, 20);
        when(videoRepository.search(
                eq("%고양이%"),
                eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(video), requestedPage, 1));

        var response = videoSearchService.search("  고양이  ", null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).videoId()).isEqualTo(152L);
        assertThat(response.content().get(0).channelName()).isEqualTo("냥이TV");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).search(
                eq("%고양이%"),
                eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort().toString())
                .isEqualTo("createdAt: DESC,id: DESC");
    }

    @Test
    void treatsLikeWildcardsAsLiteralCharacters() {
        when(videoRepository.search(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        videoSearchService.search("100%_video", "0", "20");

        verify(videoRepository).search(
                eq("%100\\%\\_video%"),
                eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED),
                any(Pageable.class)
        );
    }

    @Test
    void rejectsBlankKeyword() {
        assertBusinessError(
                () -> videoSearchService.search("   ", null, null),
                ErrorCode.REQUIRED_FIELD_MISSING
        );
        verify(videoRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidPage() {
        assertBusinessError(
                () -> videoSearchService.search("고양이", "-1", "20"),
                ErrorCode.INVALID_PAGE_NUMBER
        );
        assertBusinessError(
                () -> videoSearchService.search("고양이", "first", "20"),
                ErrorCode.INVALID_PAGE_NUMBER
        );
    }

    @Test
    void rejectsInvalidSize() {
        assertBusinessError(
                () -> videoSearchService.search("고양이", "0", "0"),
                ErrorCode.INVALID_PAGE_SIZE
        );
        assertBusinessError(
                () -> videoSearchService.search("고양이", "0", "101"),
                ErrorCode.INVALID_PAGE_SIZE
        );
        assertBusinessError(
                () -> videoSearchService.search("고양이", "0", "large"),
                ErrorCode.INVALID_PAGE_SIZE
        );
    }

    private void assertBusinessError(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private Video video() {
        Video video = org.mockito.Mockito.mock(Video.class);
        User uploader = org.mockito.Mockito.mock(User.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 19, 13, 30);

        when(video.getId()).thenReturn(152L);
        when(video.getTitle()).thenReturn("귀여운 고양이 모음");
        when(video.getThumbnailUrl()).thenReturn("https://example.com/thumbnails/152.jpg");
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getNickname()).thenReturn("냥이TV");
        when(video.getViewCount()).thenReturn(12_540L);
        when(video.getCreatedAt()).thenReturn(createdAt);
        return video;
    }
}
