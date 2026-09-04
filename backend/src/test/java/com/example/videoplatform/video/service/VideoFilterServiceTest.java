package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.category.entity.Category;
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
class VideoFilterServiceTest {

    @Mock VideoRepository videoRepository;
    @Mock CategoryRepository categoryRepository;
    private VideoFilterService service;

    @BeforeEach
    void setUp() {
        service = new VideoFilterService(videoRepository, categoryRepository);
    }

    @Test
    void combinesKeywordCategoryAndPopularSort() {
        Video video = video();
        when(categoryRepository.existsById(3L)).thenReturn(true);
        when(videoRepository.filter(eq("%스프링%"), eq(3L), eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(video), PageRequest.of(0, 20), 1));

        var response = service.filter(" 스프링 ", "3", "POPULAR", null, null);

        assertThat(response.content().get(0).categoryId()).isEqualTo(3L);
        assertThat(response.content().get(0).categoryName()).isEqualTo("교육");
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).filter(eq("%스프링%"), eq(3L), eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED), pageable.capture());
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("viewCount: DESC,id: DESC");
    }

    @Test
    void listsAllVideosWithLatestSortByDefault() {
        when(videoRepository.filter(eq(null), eq(null), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        service.filter(null, null, null, null, null);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(videoRepository).filter(eq(null), eq(null), eq(VideoVisibility.PUBLIC),
                eq(VideoStatus.PUBLISHED), pageable.capture());
        assertThat(pageable.getValue().getSort().toString()).isEqualTo("createdAt: DESC,id: DESC");
    }

    @Test
    void rejectsUnsupportedSort() {
        assertBusinessError(() -> service.filter(null, null, "RANDOM", null, null),
                ErrorCode.INVALID_VIDEO_SORT);
        verify(videoRepository, never()).filter(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsInvalidOrUnknownCategory() {
        assertBusinessError(() -> service.filter(null, "category", null, null, null),
                ErrorCode.INVALID_CATEGORY_ID);
        when(categoryRepository.existsById(3L)).thenReturn(false);
        assertBusinessError(() -> service.filter(null, "3", null, null, null),
                ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void rejectsInvalidPaging() {
        assertBusinessError(() -> service.filter(null, null, null, "-1", null),
                ErrorCode.INVALID_PAGE_NUMBER);
        assertBusinessError(() -> service.filter(null, null, null, null, "101"),
                ErrorCode.INVALID_PAGE_SIZE);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private Video video() {
        Video video = org.mockito.Mockito.mock(Video.class);
        User uploader = org.mockito.Mockito.mock(User.class);
        Category category = org.mockito.Mockito.mock(Category.class);
        when(video.getId()).thenReturn(152L);
        when(video.getTitle()).thenReturn("스프링 부트 입문 강의");
        when(video.getThumbnailUrl()).thenReturn("https://example.com/thumbnails/152.jpg");
        when(video.getCategory()).thenReturn(category);
        when(category.getId()).thenReturn(3L);
        when(category.getName()).thenReturn("교육");
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getNickname()).thenReturn("개발공부");
        when(video.getViewCount()).thenReturn(12_540L);
        when(video.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 19, 13, 30));
        return video;
    }
}
