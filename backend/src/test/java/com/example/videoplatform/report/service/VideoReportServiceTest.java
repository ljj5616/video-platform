package com.example.videoplatform.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoReportServiceTest {

    @Mock VideoReportRepository videoReportRepository;
    @Mock VideoRepository videoRepository;
    @Mock UserRepository userRepository;

    private VideoReportService videoReportService;

    @BeforeEach
    void setUp() {
        videoReportService = new VideoReportService(videoReportRepository, videoRepository, userRepository);
    }

    @Test
    void reportsVideo() {
        Video video = org.mockito.Mockito.mock(Video.class);
        User reporter = org.mockito.Mockito.mock(User.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(userRepository.findById(27L)).thenReturn(Optional.of(reporter));

        videoReportService.report(27L, "152", new VideoReportRequest("SPAM", " 반복 광고 "));

        ArgumentCaptor<VideoReport> captor = ArgumentCaptor.forClass(VideoReport.class);
        verify(videoReportRepository).save(captor.capture());
        assertThat(captor.getValue().getReporter()).isSameAs(reporter);
        assertThat(captor.getValue().getVideo()).isSameAs(video);
        assertThat(captor.getValue().getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(captor.getValue().getDescription()).isEqualTo("반복 광고");
    }

    @Test
    void rejectsUnsupportedReason() {
        assertBusinessError(
                () -> videoReportService.report(27L, "152", new VideoReportRequest("HARASSMENT", null)),
                ErrorCode.INVALID_REPORT_REASON);
        verify(videoRepository, never()).findByIdAndDeletedAtIsNull(any());
    }

    @Test
    void requiresDescriptionForOtherReason() {
        assertBusinessError(
                () -> videoReportService.report(27L, "152", new VideoReportRequest("OTHER", "  ")),
                ErrorCode.REPORT_DESCRIPTION_REQUIRED);
    }

    @Test
    void rejectsLongDescription() {
        assertBusinessError(
                () -> videoReportService.report(27L, "152", new VideoReportRequest("SPAM", "a".repeat(1_001))),
                ErrorCode.INVALID_REPORT_DESCRIPTION);
    }

    @Test
    void rejectsDuplicateReport() {
        Video video = org.mockito.Mockito.mock(Video.class);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.of(video));
        when(videoReportRepository.existsByReporterIdAndVideoId(27L, 152L)).thenReturn(true);

        assertBusinessError(
                () -> videoReportService.report(27L, "152", new VideoReportRequest("COPYRIGHT", null)),
                ErrorCode.VIDEO_ALREADY_REPORTED);
        verify(videoReportRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void rejectsInvalidAndMissingVideo() {
        assertBusinessError(
                () -> videoReportService.report(27L, "video", new VideoReportRequest("SPAM", null)),
                ErrorCode.INVALID_VIDEO_ID);
        when(videoRepository.findByIdAndDeletedAtIsNull(152L)).thenReturn(Optional.empty());
        assertBusinessError(
                () -> videoReportService.report(27L, "152", new VideoReportRequest("SPAM", null)),
                ErrorCode.VIDEO_NOT_FOUND);
    }

    private void assertBusinessError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
