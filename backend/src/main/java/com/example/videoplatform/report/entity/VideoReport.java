package com.example.videoplatform.report.entity;

import com.example.videoplatform.global.entity.BaseTimeEntity;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.video.entity.Video;
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
@Table(name = "video_reports", uniqueConstraints =
        @UniqueConstraint(name = "uk_video_reports_reporter_video", columnNames = {"reporter_id", "video_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    private LocalDateTime resolvedAt;

    public static VideoReport create(User reporter, Video video, ReportReason reason, String description) {
        VideoReport report = new VideoReport();
        report.reporter = reporter;
        report.video = video;
        report.reason = reason;
        report.description = description;
        report.status = ReportStatus.PENDING;
        return report;
    }
}
