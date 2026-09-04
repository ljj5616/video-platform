package com.example.videoplatform.comment.controller;

import com.example.videoplatform.comment.dto.CommentPageResponse;
import com.example.videoplatform.comment.dto.CommentRequest;
import com.example.videoplatform.comment.dto.CommentResponse;
import com.example.videoplatform.comment.service.CommentService;
import com.example.videoplatform.report.dto.CommentReportRequest;
import com.example.videoplatform.report.service.CommentReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;
    private final CommentReportService commentReportService;

    public CommentController(CommentService commentService, CommentReportService commentReportService) {
        this.commentService = commentService;
        this.commentReportService = commentReportService;
    }

    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<Void> report(
            @AuthenticationPrincipal Long userId,
            @PathVariable String commentId,
            @RequestBody CommentReportRequest request
    ) {
        commentReportService.report(userId, commentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/videos/{videoId}/comments")
    public ResponseEntity<CommentResponse> create(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId,
            @RequestBody CommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(userId, videoId, request.content()));
    }

    @GetMapping("/videos/{videoId}/comments")
    public ResponseEntity<CommentPageResponse> getAll(
            @PathVariable String videoId,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size
    ) {
        return ResponseEntity.ok(commentService.getAll(videoId, page, size));
    }

    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable String commentId,
            @RequestBody CommentRequest request
    ) {
        return ResponseEntity.ok(commentService.update(userId, commentId, request.content()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable String commentId
    ) {
        commentService.delete(userId, commentId);
        return ResponseEntity.noContent().build();
    }
}
