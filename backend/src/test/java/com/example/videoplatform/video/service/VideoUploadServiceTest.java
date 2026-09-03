package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.category.entity.Category;
import com.example.videoplatform.category.repository.CategoryRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

@ExtendWith(MockitoExtension.class)
class VideoUploadServiceTest {
    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock VideoStorage videoStorage;
    @Mock VideoProcessingService videoProcessingService;

    private VideoUploadService service;

    @BeforeEach
    void setUp() {
        service = new VideoUploadService(videoRepository, userRepository, categoryRepository,
                videoStorage, videoProcessingService, DataSize.ofGigabytes(1),
                DataSize.ofMegabytes(10), "https://cdn.example.com/default.jpg");
    }

    @Test
    void storesOriginalAndStartsAsyncProcessingWithDefaultThumbnail() {
        User user = org.mockito.Mockito.mock(User.class);
        Category category = org.mockito.Mockito.mock(Category.class);
        Video saved = org.mockito.Mockito.mock(Video.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(videoStorage.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenReturn("https://s3.example.com/original.mp4");
        when(videoRepository.save(any(Video.class))).thenReturn(saved);
        when(saved.getId()).thenReturn(152L);
        when(saved.getStatus()).thenReturn(VideoStatus.PROCESSING);
        when(saved.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 9, 3, 20, 0));

        var response = service.upload(7L, video("lesson.mp4", "video/mp4"), null,
                "  강의 영상  ", "  설명  ", 3L, "public");

        assertThat(response.videoId()).isEqualTo(152L);
        assertThat(response.status()).isEqualTo("PROCESSING");
        ArgumentCaptor<Video> captor = ArgumentCaptor.forClass(Video.class);
        verify(videoRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("강의 영상");
        assertThat(captor.getValue().getDescription()).isEqualTo("설명");
        assertThat(captor.getValue().getVisibility()).isEqualTo(VideoVisibility.PUBLIC);
        assertThat(captor.getValue().getThumbnailUrl()).isEqualTo("https://cdn.example.com/default.jpg");
        verify(videoProcessingService).process(org.mockito.ArgumentMatchers.eq(152L),
                org.mockito.ArgumentMatchers.startsWith("videos/original/7/"), anyString());
    }

    @Test
    void rejectsUnsupportedVideoBeforeDatabaseAndStorageAccess() {
        var invalid = video("malware.exe", "application/octet-stream");

        assertThatThrownBy(() -> service.upload(7L, invalid, null, "제목", null, 3L, "PUBLIC"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNSUPPORTED_VIDEO_FORMAT));

        verify(userRepository, never()).findById(anyLong());
        verify(videoStorage, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    @Test
    void rejectsMissingCategoryBeforeUploadingFile() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(org.mockito.Mockito.mock(User.class)));
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upload(7L, video("lesson.webm", "video/webm"), null,
                "제목", null, 99L, "PRIVATE"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND));

        verify(videoStorage, never()).upload(anyString(), any(), anyLong(), anyString());
    }

    private MockMultipartFile video(String name, String contentType) {
        return new MockMultipartFile("videoFile", name, contentType, new byte[]{1, 2, 3});
    }
}
