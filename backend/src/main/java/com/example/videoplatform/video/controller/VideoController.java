package com.example.videoplatform.video.controller;

import com.example.videoplatform.reaction.like.service.VideoLikeService;
import com.example.videoplatform.video.dto.VideoDetailResponse;
import com.example.videoplatform.video.dto.VideoPlaybackResponse;
import com.example.videoplatform.video.service.VideoDetailService;
import com.example.videoplatform.video.service.VideoDeleteService;
import com.example.videoplatform.video.service.VideoPlaybackService;
import com.example.videoplatform.video.dto.VideoSearchResponse;
import com.example.videoplatform.video.service.VideoSearchService;
import com.example.videoplatform.video.dto.VideoUploadResponse;
import com.example.videoplatform.video.dto.VideoUpdateResponse;
import com.example.videoplatform.video.service.VideoUploadService;
import com.example.videoplatform.video.service.VideoUpdateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoSearchService videoSearchService;
    private final VideoDetailService videoDetailService;
    private final VideoDeleteService videoDeleteService;
    private final VideoPlaybackService videoPlaybackService;
    private final VideoUploadService videoUploadService;
    private final VideoUpdateService videoUpdateService;
    private final VideoLikeService videoLikeService;

    public VideoController(
            VideoSearchService videoSearchService,
            VideoDetailService videoDetailService,
            VideoDeleteService videoDeleteService,
            VideoPlaybackService videoPlaybackService,
            VideoUploadService videoUploadService,
            VideoUpdateService videoUpdateService,
            VideoLikeService videoLikeService
    ) {
        this.videoSearchService = videoSearchService;
        this.videoDetailService = videoDetailService;
        this.videoDeleteService = videoDeleteService;
        this.videoPlaybackService = videoPlaybackService;
        this.videoUploadService = videoUploadService;
        this.videoUpdateService = videoUpdateService;
        this.videoLikeService = videoLikeService;
    }

    @PostMapping("/{videoId}/likes")
    public ResponseEntity<Void> like(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
    ) {
        videoLikeService.like(userId, videoId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{videoId}/likes")
    public ResponseEntity<Void> unlike(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
    ) {
        videoLikeService.unlike(userId, videoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(value = "/{videoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoUpdateResponse> update(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long categoryId,
            @RequestPart(required = false) MultipartFile thumbnailFile,
            @RequestParam(required = false) String visibility
    ) {
        return ResponseEntity.ok(videoUpdateService.update(userId, videoId, title, description,
                categoryId, thumbnailFile, visibility));
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

    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
    ) {
        videoDeleteService.delete(userId, videoId);
        return ResponseEntity.noContent().build();
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
