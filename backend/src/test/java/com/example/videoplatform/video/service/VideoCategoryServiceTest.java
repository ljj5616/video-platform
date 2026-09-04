package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.category.repository.CategoryRepository;
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
class VideoCategoryServiceTest {

    @Mock VideoRepository videoRepository;
    @Mock CategoryRepository categoryRepository;
    private VideoCategoryService service;

    @BeforeEach
    void setUp() {
        service = new VideoCategoryService(videoRepository, categoryRepository);
    }

    @Test
    void getsPublishedPublicVideosInCategoryWithDefaults() {
        Video video = video();
        when(categoryRepository.existsById(3L)).thenReturn(true);
        when(videoRepository.findByCategory_IdAndVisibilityAndStatusAndDeletedAtIsNull(
                eq(3L), eq(VideoVisibility.PUBLIC), eq(VideoStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1));

        var response = service.getByCategory("3", null, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).videoId()).isEqualTo(152L);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).findByCategory_IdAndVisibilityAndStatusAndDeletedAtIsNull(
                eq(3L), eq(VideoVisibility.PUBLIC), eq(VideoStatus.PUBLISHED), pageable.capture());
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
    }

    @Test
    void rejectsMissingOrInvalidCategoryId() {
        assertBusinessError(() -> service.getByCategory(null, null, null), ErrorCode.REQUIRED_FIELD_MISSING);
        assertBusinessError(() -> service.getByCategory("category", null, null), ErrorCode.INVALID_CATEGORY_ID);
        assertBusinessError(() -> service.getByCategory("0", null, null), ErrorCode.INVALID_CATEGORY_ID);
        verify(videoRepository, never()).findByCategory_IdAndVisibilityAndStatusAndDeletedAtIsNull(
                any(), any(), any(), any());
    }

    @Test
    void rejectsUnknownCategory() {
        when(categoryRepository.existsById(3L)).thenReturn(false);
        assertBusinessError(() -> service.getByCategory("3", null, null), ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void rejectsInvalidPaging() {
        assertBusinessError(() -> service.getByCategory("3", "-1", null), ErrorCode.INVALID_PAGE_NUMBER);
        assertBusinessError(() -> service.getByCategory("3", null, "101"), ErrorCode.INVALID_PAGE_SIZE);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private Video video() {
        Video video = org.mockito.Mockito.mock(Video.class);
        User uploader = org.mockito.Mockito.mock(User.class);
        when(video.getId()).thenReturn(152L);
        when(video.getTitle()).thenReturn("자바 입문 강의");
        when(video.getThumbnailUrl()).thenReturn("https://example.com/thumbnails/152.jpg");
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getNickname()).thenReturn("개발공부");
        when(video.getViewCount()).thenReturn(12_540L);
        when(video.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 19, 13, 30));
        return video;
    }
}
