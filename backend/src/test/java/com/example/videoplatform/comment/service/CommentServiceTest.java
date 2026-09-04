package com.example.videoplatform.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.comment.dto.CommentPageResponse;
import com.example.videoplatform.comment.dto.CommentResponse;
import com.example.videoplatform.comment.entity.Comment;
import com.example.videoplatform.comment.entity.CommentStatus;
import com.example.videoplatform.comment.repository.CommentRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.util.Optional;
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
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, videoRepository, userRepository);
    }

    @Test
    void createsComment() {
        Video video = mock(Video.class);
        User user = mock(User.class);
        when(video.getId()).thenReturn(152L);
        when(user.getId()).thenReturn(27L);
        when(user.getNickname()).thenReturn("코딩초보");
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(userRepository.findById(27L)).thenReturn(Optional.of(user));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommentResponse response = commentService.create(27L, "152", " 유익한 영상이네요! ");

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("유익한 영상이네요!");
        assertThat(response.videoId()).isEqualTo(152L);
        assertThat(response.author().userId()).isEqualTo(27L);
    }

    @Test
    void returnsEmptyCommentPage() {
        Video video = mock(Video.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(commentRepository.findByVideoIdAndStatus(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 20), 0));

        CommentPageResponse response = commentService.getAll("152", null, null);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.last()).isTrue();
    }

    @Test
    void updatesOwnComment() {
        User owner = mock(User.class);
        Video video = mock(Video.class);
        Comment comment = new Comment(video, owner, "before");
        when(owner.getId()).thenReturn(27L);
        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));

        commentService.update(27L, "381", "after");

        assertThat(comment.getContent()).isEqualTo("after");
        verify(commentRepository).flush();
    }

    @Test
    void rejectsUpdatingAnotherUsersComment() {
        User owner = mock(User.class);
        Comment comment = new Comment(mock(Video.class), owner, "content");
        when(owner.getId()).thenReturn(27L);
        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));

        assertBusinessError(() -> commentService.update(28L, "381", "changed"),
                ErrorCode.COMMENT_ACCESS_DENIED);
        verify(commentRepository, never()).flush();
    }

    @Test
    void softDeletesOwnComment() {
        User owner = mock(User.class);
        Comment comment = new Comment(mock(Video.class), owner, "content");
        when(owner.getId()).thenReturn(27L);
        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));

        commentService.delete(27L, "381");

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
        assertThat(comment.getDeletedAt()).isNotNull();
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void rejectsMissingAndOversizedContent() {
        assertBusinessError(() -> commentService.create(27L, "152", "  "),
                ErrorCode.REQUIRED_FIELD_MISSING);

        assertBusinessError(() -> commentService.create(27L, "152", "a".repeat(1001)),
                ErrorCode.INVALID_COMMENT_CONTENT);
    }

    @Test
    void rejectsInvalidIdsAndPaging() {
        assertBusinessError(() -> commentService.getAll("video", null, null), ErrorCode.INVALID_VIDEO_ID);
        assertBusinessError(() -> commentService.delete(27L, "0"), ErrorCode.INVALID_COMMENT_ID);

        Video video = mock(Video.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        assertBusinessError(() -> commentService.getAll("152", "-1", null), ErrorCode.INVALID_PAGE_NUMBER);
        assertBusinessError(() -> commentService.getAll("152", null, "101"), ErrorCode.INVALID_PAGE_SIZE);
    }

    @Test
    void rejectsMissingVideoAndComment() {
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.empty());
        assertBusinessError(() -> commentService.getAll("152", null, null), ErrorCode.VIDEO_NOT_FOUND);

        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE))
                .thenReturn(Optional.empty());
        assertBusinessError(() -> commentService.delete(27L, "381"), ErrorCode.COMMENT_NOT_FOUND);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
