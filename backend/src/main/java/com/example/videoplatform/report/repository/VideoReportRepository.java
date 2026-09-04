package com.example.videoplatform.report.repository;

import com.example.videoplatform.report.entity.VideoReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoReportRepository extends JpaRepository<VideoReport, Long> {

    boolean existsByReporterIdAndVideoId(Long reporterId, Long videoId);
}
