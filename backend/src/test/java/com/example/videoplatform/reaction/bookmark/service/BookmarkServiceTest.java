package com.example.videoplatform.reaction.bookmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.reaction.bookmark.entity.Bookmark;
import com.example.videoplatform.reaction.bookmark.entity.BookmarkId;
import com.example.videoplatform.reaction.bookmark.repository.BookmarkRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock BookmarkRepository bookmarkRepository;
    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;

    private BookmarkService bookmarkService;

    @BeforeEach
    void setUp() {
        bookmarkService = new BookmarkService(bookmarkRepository, videoRepository, userRepository);
    }

    @Test
    void addsBookmark() {
        Video video = mock(Video.class);
        User user = mock(User.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(userRepository.findById(27L)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(27L);
        when(video.getId()).thenReturn(152L);

        bookmarkService.add(27L, "152");

        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void rejectsDuplicateBookmark() {
        Video video = mock(Video.class);
        BookmarkId id = new BookmarkId(27L, 152L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(bookmarkRepository.existsById(id)).thenReturn(true);

        assertBusinessError(() -> bookmarkService.add(27L, "152"), ErrorCode.VIDEO_ALREADY_BOOKMARKED);
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void removesBookmark() {
        Video video = mock(Video.class);
        Bookmark bookmark = mock(Bookmark.class);
        BookmarkId id = new BookmarkId(27L, 152L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(bookmarkRepository.findById(id)).thenReturn(Optional.of(bookmark));

        bookmarkService.remove(27L, "152");

        verify(bookmarkRepository).delete(bookmark);
    }

    @Test
    void rejectsRemoveWithoutExistingBookmark() {
        Video video = mock(Video.class);
        BookmarkId id = new BookmarkId(27L, 152L);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(bookmarkRepository.findById(id)).thenReturn(Optional.empty());

        assertBusinessError(() -> bookmarkService.remove(27L, "152"), ErrorCode.VIDEO_BOOKMARK_NOT_FOUND);
    }

    @Test
    void rejectsInvalidVideoIdBeforeRepositoryLookup() {
        assertBusinessError(() -> bookmarkService.add(27L, "video"), ErrorCode.INVALID_VIDEO_ID);
        assertBusinessError(() -> bookmarkService.remove(27L, "0"), ErrorCode.INVALID_VIDEO_ID);
        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void rejectsMissingVideo() {
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.empty());

        assertBusinessError(() -> bookmarkService.add(27L, "152"), ErrorCode.VIDEO_NOT_FOUND);
        assertBusinessError(() -> bookmarkService.remove(27L, "152"), ErrorCode.VIDEO_NOT_FOUND);
    }

    @Test
    void getsBookmarksWithDefaultPaging() {
        when(bookmarkRepository.findByUser_IdOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.eq(27L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = bookmarkService.getBookmarks(27L, null, null);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void rejectsInvalidBookmarkPaging() {
        assertBusinessError(() -> bookmarkService.getBookmarks(27L, "-1", null),
                ErrorCode.INVALID_PAGE_NUMBER);
        assertBusinessError(() -> bookmarkService.getBookmarks(27L, "page", null),
                ErrorCode.INVALID_PAGE_NUMBER);
        assertBusinessError(() -> bookmarkService.getBookmarks(27L, null, "0"),
                ErrorCode.INVALID_PAGE_SIZE);
        assertBusinessError(() -> bookmarkService.getBookmarks(27L, null, "101"),
                ErrorCode.INVALID_PAGE_SIZE);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
