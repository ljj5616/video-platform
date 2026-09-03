package com.example.videoplatform.video.controller;

import com.example.videoplatform.video.dto.VideoDetailResponse;
import com.example.videoplatform.video.dto.VideoPlaybackResponse;
import com.example.videoplatform.video.service.VideoDetailService;
import com.example.videoplatform.video.service.VideoPlaybackService;
import com.example.videoplatform.video.dto.VideoSearchResponse;
import com.example.videoplatform.video.service.VideoSearchService;
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

    public VideoController(
            VideoSearchService videoSearchService,
            VideoDetailService videoDetailService,
            VideoPlaybackService videoPlaybackService
    ) {
        this.videoSearchService = videoSearchService;
        this.videoDetailService = videoDetailService;
        this.videoPlaybackService = videoPlaybackService;
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
