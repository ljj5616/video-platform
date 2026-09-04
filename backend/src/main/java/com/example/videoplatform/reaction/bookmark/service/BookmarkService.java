package com.example.videoplatform.reaction.bookmark.service;

import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.reaction.bookmark.entity.Bookmark;
import com.example.videoplatform.reaction.bookmark.entity.BookmarkId;
import com.example.videoplatform.reaction.bookmark.repository.BookmarkRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookmarkService {

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
}
