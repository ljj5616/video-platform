package com.example.videoplatform.report.entity;

import com.example.videoplatform.comment.entity.Comment;
import com.example.videoplatform.global.entity.BaseTimeEntity;
import com.example.videoplatform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "comment_reports", uniqueConstraints =
        @UniqueConstraint(name = "uk_comment_reports_reporter_comment", columnNames = {"reporter_id", "comment_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    private LocalDateTime resolvedAt;

    public static CommentReport create(User reporter, Comment comment, ReportReason reason, String description) {
        CommentReport report = new CommentReport();
        report.reporter = reporter;
        report.comment = comment;
        report.reason = reason;
        report.description = description;
        report.status = ReportStatus.PENDING;
        return report;
    }
}
