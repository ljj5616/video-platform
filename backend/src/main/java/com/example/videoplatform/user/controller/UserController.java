package com.example.videoplatform.user.controller;

import com.example.videoplatform.user.dto.UserSignUpRequest;
import com.example.videoplatform.user.dto.UserSignUpResponse;
import com.example.videoplatform.user.dto.UserWithdrawalRequest;
import com.example.videoplatform.history.dto.WatchHistoryResponse;
import com.example.videoplatform.history.service.WatchHistoryService;
import com.example.videoplatform.reaction.bookmark.dto.BookmarkListResponse;
import com.example.videoplatform.reaction.bookmark.service.BookmarkService;
import com.example.videoplatform.reaction.like.dto.VideoLikeListResponse;
import com.example.videoplatform.reaction.like.service.VideoLikeService;
import com.example.videoplatform.user.service.UserService;
import com.example.videoplatform.user.service.UserWithdrawalService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserWithdrawalService userWithdrawalService;
    private final WatchHistoryService watchHistoryService;
    private final BookmarkService bookmarkService;
    private final VideoLikeService videoLikeService;

    public UserController(UserService userService, UserWithdrawalService userWithdrawalService,
                          WatchHistoryService watchHistoryService, BookmarkService bookmarkService,
                          VideoLikeService videoLikeService) {
        this.userService = userService;
        this.userWithdrawalService = userWithdrawalService;
        this.watchHistoryService = watchHistoryService;
        this.bookmarkService = bookmarkService;
        this.videoLikeService = videoLikeService;
    }

    @GetMapping("/me/likes")
    public ResponseEntity<VideoLikeListResponse> getLikes(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size
    ) {
        return ResponseEntity.ok(videoLikeService.getLikes(userId, page, size));
    }

    @GetMapping("/me/bookmarks")
    public ResponseEntity<BookmarkListResponse> getBookmarks(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size
    ) {
        return ResponseEntity.ok(bookmarkService.getBookmarks(userId, page, size));
    }

    @PostMapping
    public ResponseEntity<UserSignUpResponse> signUp(@Valid @RequestBody UserSignUpRequest request) {
        UserSignUpResponse response = userService.signUp(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UserWithdrawalRequest request
    ) {
        userWithdrawalService.withdraw(userId, request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/watch-history")
    public ResponseEntity<WatchHistoryResponse> getWatchHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size
    ) {
        return ResponseEntity.ok(watchHistoryService.getHistory(userId, page, size));
    }

    @DeleteMapping("/me/watch-history/{videoId}")
    public ResponseEntity<Void> deleteWatchHistory(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
    ) {
        watchHistoryService.deleteHistory(userId, videoId);
        return ResponseEntity.noContent().build();
    }
}
