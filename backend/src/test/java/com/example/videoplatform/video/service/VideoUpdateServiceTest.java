package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
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
import com.example.videoplatform.video.storage.VideoStorage;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class VideoUpdateServiceTest {
    @Mock VideoRepository videoRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock VideoStorage videoStorage;

    private VideoUpdateService service;

    @BeforeEach
    void setUp() {
        service = new VideoUpdateService(videoRepository, categoryRepository, videoStorage,
                DataSize.ofMegabytes(10));
    }

    @Test
    void updatesOnlyProvidedFieldsAndReturnsUpdatedVideo() {
        Video video = editableVideo(7L);
        Category newCategory = category(4L, "프로그래밍");
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(newCategory));
        when(video.getId()).thenReturn(152L);
        when(video.getTitle()).thenReturn("스프링 부트 기초 강의");
        when(video.getDescription()).thenReturn("기존 설명");
        when(video.getThumbnailUrl()).thenReturn("https://cdn.example.com/old.jpg");
        when(video.getCategory()).thenReturn(newCategory);
        when(video.getVisibility()).thenReturn(VideoVisibility.PRIVATE);
        when(video.getUpdatedAt()).thenReturn(LocalDateTime.of(2026, 8, 21, 14, 20));

        var response = service.update(7L, "152", "  스프링 부트 기초 강의  ", null,
                4L, null, "private");

        verify(video).update("스프링 부트 기초 강의", null, newCategory, VideoVisibility.PRIVATE, null);
        verify(videoRepository).flush();
        assertThat(response.videoId()).isEqualTo(152L);
        assertThat(response.categoryId()).isEqualTo(4L);
        assertThat(response.visibility()).isEqualTo("PRIVATE");
    }

    @Test
    void uploadsAndAppliesNewThumbnail() {
        Video video = editableVideo(7L);
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnailFile", "cover.webp", "image/webp", new byte[]{1, 2, 3});
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(videoStorage.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://cdn.example.com/new.webp");
        stubResponse(video);

        service.update(7L, "152", null, null, null, thumbnail, null);

        verify(video).update(null, null, null, null, "https://cdn.example.com/new.webp");
    }

    @Test
    void rejectsUserWhoIsNotUploader() {
        Video video = editableVideo(7L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> service.update(99L, "152", "제목", null,
                null, null, null), ErrorCode.VIDEO_ACCESS_DENIED);

        verify(video, never()).update(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsVideoWhileProcessing() {
        Video video = video(7L, VideoStatus.PROCESSING);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> service.update(7L, "152", "제목", null,
                null, null, null), ErrorCode.VIDEO_NOT_EDITABLE);
    }

    @Test
    void rejectsRequestWithoutAnyUpdateField() {
        assertBusinessError(() -> service.update(7L, "152", null, null,
                null, null, null), ErrorCode.REQUIRED_FIELD_MISSING);

        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(anyLong());
    }

    @Test
    void rejectsUnsupportedThumbnailBeforeDatabaseAccess() {
        MockMultipartFile invalid = new MockMultipartFile(
                "thumbnailFile", "cover.gif", "image/gif", new byte[]{1});

        assertBusinessError(() -> service.update(7L, "152", null, null,
                null, invalid, null), ErrorCode.UNSUPPORTED_THUMBNAIL_FORMAT);

        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(anyLong());
    }

    private Video editableVideo(long uploaderId) {
        return video(uploaderId, VideoStatus.PUBLISHED);
    }

    private Video video(long uploaderId, VideoStatus status) {
        Video video = mock(Video.class);
        User uploader = mock(User.class);
        when(uploader.getId()).thenReturn(uploaderId);
        when(video.getUploader()).thenReturn(uploader);
        lenient().when(video.getStatus()).thenReturn(status);
        return video;
    }

    private Category category(long id, String name) {
        Category category = mock(Category.class);
        when(category.getId()).thenReturn(id);
        when(category.getName()).thenReturn(name);
        return category;
    }

    private void stubResponse(Video video) {
        Category category = category(3L, "프로그래밍");
        when(video.getId()).thenReturn(152L);
        when(video.getTitle()).thenReturn("제목");
        when(video.getThumbnailUrl()).thenReturn("https://cdn.example.com/new.webp");
        when(video.getCategory()).thenReturn(category);
        when(video.getVisibility()).thenReturn(VideoVisibility.PUBLIC);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
