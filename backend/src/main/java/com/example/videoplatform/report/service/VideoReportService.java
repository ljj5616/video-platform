package com.example.videoplatform.report.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.report.dto.VideoReportRequest;
import com.example.videoplatform.report.entity.ReportReason;
import com.example.videoplatform.report.entity.VideoReport;
import com.example.videoplatform.report.repository.VideoReportRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VideoReportService {

    private static final int MAX_DESCRIPTION_LENGTH = 1_000;

    private final VideoReportRepository videoReportRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public VideoReportService(VideoReportRepository videoReportRepository,
                              VideoRepository videoRepository,
                              UserRepository userRepository) {
        this.videoReportRepository = videoReportRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void report(Long userId, String videoIdValue, VideoReportRequest request) {
        long videoId = parseVideoId(videoIdValue);
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }

        ReportReason reason = parseReason(request.reason());
        String description = normalizeDescription(request.description());
        if (reason == ReportReason.OTHER && description == null) {
            throw new BusinessException(ErrorCode.REPORT_DESCRIPTION_REQUIRED);
        }

        Video video = videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
        if (videoReportRepository.existsByReporterIdAndVideoId(userId, videoId)) {
            throw new BusinessException(ErrorCode.VIDEO_ALREADY_REPORTED);
        }
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        videoReportRepository.save(VideoReport.create(reporter, video, reason, description));
    }

    private long parseVideoId(String value) {
        try {
            long videoId = Long.parseLong(value);
            if (videoId < 1) throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
            return videoId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.INVALID_VIDEO_ID);
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
