package com.example.videoplatform.video.controller;

import com.example.videoplatform.video.dto.VideoSearchResponse;
import com.example.videoplatform.video.service.VideoSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoSearchService videoSearchService;

    public VideoController(VideoSearchService videoSearchService) {
        this.videoSearchService = videoSearchService;
    }

    @GetMapping
    public ResponseEntity<VideoSearchResponse> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size
    ) {
        return ResponseEntity.ok(videoSearchService.search(keyword, page, size));
    }
}
