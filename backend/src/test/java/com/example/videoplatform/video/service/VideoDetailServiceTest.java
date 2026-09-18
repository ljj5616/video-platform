package com.example.videoplatform.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.category.entity.Category;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import com.example.videoplatform.video.repository.VideoRepository;
import com.example.videoplatform.reaction.like.repository.VideoLikeRepository;
import com.example.videoplatform.reaction.like.entity.VideoLikeId;
import com.example.videoplatform.reaction.bookmark.repository.BookmarkRepository;
import com.example.videoplatform.reaction.bookmark.entity.BookmarkId;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoDetailServiceTest {

    @Mock
    private VideoRepository videoRepository;
    @Mock
    private VideoLikeRepository videoLikeRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;

    private VideoDetailService videoDetailService;

    @BeforeEach
    void setUp() {
        videoDetailService = new VideoDetailService(videoRepository, videoLikeRepository, bookmarkRepository);
    }

    @Test
    void returnsCurrentUsersReactions() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(video.getLikeCount()).thenReturn(12L);
        when(videoLikeRepository.existsById(new VideoLikeId(99L, 152L))).thenReturn(true);
        when(bookmarkRepository.existsById(new BookmarkId(99L, 152L))).thenReturn(true);
        var response = videoDetailService.getDetail(99L, "152");
        assertThat(response.likeCount()).isEqualTo(12L);
        assertThat(response.liked()).isTrue();
        assertThat(response.bookmarked()).isTrue();
    }

    @Test
    void anonymousDetailDoesNotQueryPersonalReactions() {
        Video publicVideo = video(VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L))
                .thenReturn(Optional.of(publicVideo));
        var response = videoDetailService.getDetail(null, "152");
        assertThat(response.liked()).isFalse();
        assertThat(response.bookmarked()).isFalse();
        org.mockito.Mockito.verifyNoInteractions(videoLikeRepository, bookmarkRepository);
    }

    @Test
    void returnsPublishedPublicVideoDetail() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PUBLISHED, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        var response = videoDetailService.getDetail(99L, "152");

        assertThat(response.videoId()).isEqualTo(152L);
        assertThat(response.title()).isEqualTo("스프링 부트 입문 강의");
        assertThat(response.description()).isNull();
        assertThat(response.duration()).isEqualTo(634L);
        assertThat(response.categoryId()).isEqualTo(3L);
        assertThat(response.categoryName()).isEqualTo("교육");
        assertThat(response.author().userId()).isEqualTo(27L);
        assertThat(response.author().nickname()).isEqualTo("개발공부");
        assertThat(response.author().profileImageUrl()).isNull();
    }

    @Test
    void allowsPublishedUnlistedVideoForOtherUser() {
        Video video = video(VideoVisibility.UNLISTED, VideoStatus.PUBLISHED, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertThat(videoDetailService.getDetail(99L, "152").videoId()).isEqualTo(152L);
    }

    @Test
    void allowsUploaderToViewPrivateAndUnpublishedVideo() {
        Video video = video(VideoVisibility.PRIVATE, VideoStatus.PROCESSING, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertThat(videoDetailService.getDetail(27L, "152").videoId()).isEqualTo(152L);
    }

    @Test
    void rejectsPrivateVideoForOtherUser() {
        Video video = video(VideoVisibility.PRIVATE, VideoStatus.PUBLISHED, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> videoDetailService.getDetail(99L, "152"), ErrorCode.VIDEO_ACCESS_DENIED);
    }

    @Test
    void rejectsUnpublishedVideoForOtherUser() {
        Video video = video(VideoVisibility.PUBLIC, VideoStatus.PROCESSING, 27L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));

        assertBusinessError(() -> videoDetailService.getDetail(99L, "152"), ErrorCode.VIDEO_ACCESS_DENIED);
    }

    @Test
    void rejectsInvalidVideoIdBeforeRepositoryLookup() {
        assertBusinessError(() -> videoDetailService.getDetail(99L, "video"), ErrorCode.INVALID_VIDEO_ID);
        assertBusinessError(() -> videoDetailService.getDetail(99L, "0"), ErrorCode.INVALID_VIDEO_ID);
        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsMissingVideo() {
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.empty());

        assertBusinessError(() -> videoDetailService.getDetail(99L, "152"), ErrorCode.VIDEO_NOT_FOUND);
    }

    private void assertBusinessError(Runnable action, ErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }

    private Video video(VideoVisibility visibility, VideoStatus status, Long uploaderId) {
        Video video = mock(Video.class);
        User uploader = mock(User.class);
        Category category = mock(Category.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 19, 13, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 19, 14, 10);

        lenient().when(video.getId()).thenReturn(152L);
        lenient().when(video.getTitle()).thenReturn("스프링 부트 입문 강의");
        lenient().when(video.getDescription()).thenReturn(null);
        lenient().when(video.getThumbnailUrl()).thenReturn("https://example.com/thumbnails/152.jpg");
        lenient().when(video.getDurationSeconds()).thenReturn(634);
        lenient().when(video.getViewCount()).thenReturn(12_540L);
        lenient().when(video.getVisibility()).thenReturn(visibility);
        lenient().when(video.getStatus()).thenReturn(status);
        when(video.getUploader()).thenReturn(uploader);
        when(uploader.getId()).thenReturn(uploaderId);
        lenient().when(uploader.getNickname()).thenReturn("개발공부");
        lenient().when(uploader.getProfileImageUrl()).thenReturn(null);
        lenient().when(video.getCategory()).thenReturn(category);
        lenient().when(category.getId()).thenReturn(3L);
        lenient().when(category.getName()).thenReturn("교육");
        lenient().when(video.getCreatedAt()).thenReturn(createdAt);
        lenient().when(video.getUpdatedAt()).thenReturn(updatedAt);
        return video;
    }
}
