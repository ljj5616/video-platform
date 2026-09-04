package com.example.videoplatform.report.service;

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
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentReportService {

    private static final int MAX_DESCRIPTION_LENGTH = 1_000;

    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentReportService(CommentReportRepository commentReportRepository,
                                CommentRepository commentRepository,
                                UserRepository userRepository) {
        this.commentReportRepository = commentReportRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void report(Long userId, String commentIdValue, CommentReportRequest request) {
        long commentId = parseCommentId(commentIdValue);
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }

        ReportReason reason = parseReason(request.reason());
        String description = normalizeDescription(request.description());
        if (reason == ReportReason.OTHER && description == null) {
            throw new BusinessException(ErrorCode.REPORT_DESCRIPTION_REQUIRED);
        }

        Comment comment = commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
        if (commentReportRepository.existsByReporterIdAndCommentId(userId, commentId)) {
            throw new BusinessException(ErrorCode.VIDEO_ALREADY_REPORTED);
        }
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        commentReportRepository.save(CommentReport.create(reporter, comment, reason, description));
    }

    private long parseCommentId(String value) {
        try {
            long commentId = Long.parseLong(value);
            if (commentId < 1) throw new BusinessException(ErrorCode.INVALID_COMMENT_ID);
            return commentId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_ID);
        }
    }

    private ReportReason parseReason(String value) {
        try {
            return ReportReason.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_REASON);
        }
    }

    private String normalizeDescription(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_REPORT_DESCRIPTION);
        }
        return normalized;
    }
}
