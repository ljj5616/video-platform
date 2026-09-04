package com.example.videoplatform.report.repository;

import com.example.videoplatform.report.entity.CommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentReportRepository extends JpaRepository<CommentReport, Long> {

    boolean existsByReporterIdAndCommentId(Long reporterId, Long commentId);
}
