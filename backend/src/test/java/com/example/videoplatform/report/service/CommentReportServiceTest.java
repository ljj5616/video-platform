package com.example.videoplatform.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.videoplatform.comment.entity.Comment;
import com.example.videoplatform.comment.entity.CommentStatus;
import com.example.videoplatform.comment.repository.CommentRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.report.dto.CommentReportRequest;
import com.example.videoplatform.report.entity.CommentReport;
import com.example.videoplatform.report.entity.ReportReason;
import com.example.videoplatform.report.repository.CommentReportRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentReportServiceTest {

    @Mock CommentReportRepository commentReportRepository;
    @Mock CommentRepository commentRepository;
    @Mock UserRepository userRepository;

    private CommentReportService commentReportService;

    @BeforeEach
    void setUp() {
        commentReportService = new CommentReportService(
                commentReportRepository, commentRepository, userRepository);
    }

    @Test
    void reportsComment() {
        Comment comment = org.mockito.Mockito.mock(Comment.class);
        User reporter = org.mockito.Mockito.mock(User.class);
        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));
        when(userRepository.findById(27L)).thenReturn(Optional.of(reporter));

        commentReportService.report(27L, "381",
                new CommentReportRequest("HATE_SPEECH", " 특정 집단을 비하합니다. "));

        ArgumentCaptor<CommentReport> captor = ArgumentCaptor.forClass(CommentReport.class);
        verify(commentReportRepository).save(captor.capture());
        assertThat(captor.getValue().getReporter()).isSameAs(reporter);
        assertThat(captor.getValue().getComment()).isSameAs(comment);
        assertThat(captor.getValue().getReason()).isEqualTo(ReportReason.HATE_SPEECH);
        assertThat(captor.getValue().getDescription()).isEqualTo("특정 집단을 비하합니다.");
    }

    @Test
    void rejectsMissingAndUnsupportedReason() {
        assertBusinessError(() -> commentReportService.report(27L, "381", null),
                ErrorCode.REQUIRED_FIELD_MISSING);
        assertBusinessError(() -> commentReportService.report(27L, "381",
                new CommentReportRequest("HARASSMENT", null)), ErrorCode.INVALID_REPORT_REASON);
        verify(commentRepository, never()).findByIdAndStatus(any(), any());
    }

    @Test
    void validatesDescription() {
        assertBusinessError(() -> commentReportService.report(27L, "381",
                new CommentReportRequest("OTHER", "  ")), ErrorCode.REPORT_DESCRIPTION_REQUIRED);
        assertBusinessError(() -> commentReportService.report(27L, "381",
                new CommentReportRequest("SPAM", "a".repeat(1_001))), ErrorCode.INVALID_REPORT_DESCRIPTION);
    }

    @Test
    void rejectsDuplicateReport() {
        Comment comment = org.mockito.Mockito.mock(Comment.class);
        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE))
                .thenReturn(Optional.of(comment));
        when(commentReportRepository.existsByReporterIdAndCommentId(27L, 381L)).thenReturn(true);

        assertBusinessError(() -> commentReportService.report(27L, "381",
                new CommentReportRequest("SPAM", null)), ErrorCode.VIDEO_ALREADY_REPORTED);
        verify(commentReportRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void rejectsInvalidAndMissingComment() {
        assertBusinessError(() -> commentReportService.report(27L, "comment",
                new CommentReportRequest("SPAM", null)), ErrorCode.INVALID_COMMENT_ID);
        when(commentRepository.findByIdAndStatus(381L, CommentStatus.ACTIVE)).thenReturn(Optional.empty());
        assertBusinessError(() -> commentReportService.report(27L, "381",
                new CommentReportRequest("SPAM", null)), ErrorCode.COMMENT_NOT_FOUND);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
