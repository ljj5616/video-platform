package com.example.videoplatform.video.controller;

import com.example.videoplatform.video.dto.VideoDetailResponse;
import com.example.videoplatform.video.dto.VideoPlaybackResponse;
import com.example.videoplatform.video.service.VideoDetailService;
import com.example.videoplatform.video.service.VideoPlaybackService;
import com.example.videoplatform.video.dto.VideoSearchResponse;
import com.example.videoplatform.video.service.VideoSearchService;
import com.example.videoplatform.video.dto.VideoUploadResponse;
import com.example.videoplatform.video.service.VideoUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoSearchService videoSearchService;
    private final VideoDetailService videoDetailService;
    private final VideoPlaybackService videoPlaybackService;
    private final VideoUploadService videoUploadService;

    public VideoController(
            VideoSearchService videoSearchService,
            VideoDetailService videoDetailService,
            VideoPlaybackService videoPlaybackService,
            VideoUploadService videoUploadService
    ) {
        this.videoSearchService = videoSearchService;
        this.videoDetailService = videoDetailService;
        this.videoPlaybackService = videoPlaybackService;
        this.videoUploadService = videoUploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUploadResponse> upload(
            @AuthenticationPrincipal Long userId,
            @RequestPart MultipartFile videoFile,
            @RequestPart(required = false) MultipartFile thumbnailFile,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Long categoryId,
            @RequestParam String visibility
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(videoUploadService.upload(userId, videoFile, thumbnailFile, title,
                        description, categoryId, visibility));
    }

    @GetMapping
    public ResponseEntity<VideoSearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size
    ) {
        return ResponseEntity.ok(videoSearchService.search(keyword, page, size));
    }

    @GetMapping("/{videoId}")
    public ResponseEntity<VideoDetailResponse> getDetail(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
    ) {
        return ResponseEntity.ok(videoDetailService.getDetail(userId, videoId));
    }

    @GetMapping("/{videoId}/playback")
    public ResponseEntity<VideoPlaybackResponse> getPlayback(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
    ) {
        return ResponseEntity.ok(videoPlaybackService.getPlayback(userId, videoId));
    }
}
