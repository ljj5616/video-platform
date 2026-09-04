package com.example.videoplatform.reaction.bookmark.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.reaction.bookmark.entity.Bookmark;
import com.example.videoplatform.reaction.bookmark.entity.BookmarkId;
import com.example.videoplatform.reaction.bookmark.dto.BookmarkItemResponse;
import com.example.videoplatform.reaction.bookmark.dto.BookmarkListResponse;
import com.example.videoplatform.reaction.bookmark.repository.BookmarkRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BookmarkRepository bookmarkRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, VideoRepository videoRepository,
                           UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void add(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        Video video = findVideo(videoId);
        BookmarkId bookmarkId = new BookmarkId(userId, videoId);
        if (bookmarkRepository.existsById(bookmarkId)) {
            throw new BusinessException(ErrorCode.VIDEO_ALREADY_BOOKMARKED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        bookmarkRepository.save(new Bookmark(user, video));
    }

    @Transactional
    public void remove(Long userId, String videoIdValue) {
        long videoId = parseVideoId(videoIdValue);
        findVideo(videoId);
        BookmarkId bookmarkId = new BookmarkId(userId, videoId);
        Bookmark bookmark = bookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_BOOKMARK_NOT_FOUND));

        bookmarkRepository.delete(bookmark);
    }

    public BookmarkListResponse getBookmarks(Long userId, String pageValue, String sizeValue) {
        int page = parsePage(pageValue);
        int size = parseSize(sizeValue);
        return BookmarkListResponse.from(bookmarkRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(BookmarkItemResponse::from));
    }

    private Video findVideo(long videoId) {
        return videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
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

    private int parsePage(String value) {
        int page = parseInteger(value, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        return page;
    }

    private int parseSize(String value) {
        int size = parseInteger(value, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);
        if (size < 1 || size > MAX_SIZE) throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);
        return size;
    }

    private int parseInteger(String value, int defaultValue, ErrorCode errorCode) {
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new BusinessException(errorCode);
        }
    }
}
