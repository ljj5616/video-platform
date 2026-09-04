package com.example.videoplatform.comment.service;

import com.example.videoplatform.comment.dto.CommentPageResponse;
import com.example.videoplatform.comment.dto.CommentResponse;
import com.example.videoplatform.comment.entity.Comment;
import com.example.videoplatform.comment.entity.CommentStatus;
import com.example.videoplatform.comment.repository.CommentRepository;
import com.example.videoplatform.global.error.BusinessException;
import com.example.videoplatform.global.error.ErrorCode;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.repository.VideoRepository;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;
    private static final int MAX_CONTENT_LENGTH = 1000;

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, VideoRepository videoRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CommentResponse create(Long userId, String videoIdValue, String contentValue) {
        String content = normalizeContent(contentValue);
        Video video = findVideo(parseVideoId(videoIdValue));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Comment comment = commentRepository.save(new Comment(video, user, content));
        return CommentResponse.from(comment);
    }

    public CommentPageResponse getAll(String videoIdValue, String pageValue, String sizeValue) {
        long videoId = parseVideoId(videoIdValue);
        findVideo(videoId);
        int page = parseInteger(pageValue, DEFAULT_PAGE, ErrorCode.INVALID_PAGE_NUMBER);
        int size = parseInteger(sizeValue, DEFAULT_SIZE, ErrorCode.INVALID_PAGE_SIZE);
        if (page < 0) throw new BusinessException(ErrorCode.INVALID_PAGE_NUMBER);
        if (size < 1 || size > MAX_SIZE) throw new BusinessException(ErrorCode.INVALID_PAGE_SIZE);

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return CommentPageResponse.from(commentRepository
                .findByVideoIdAndStatus(videoId, CommentStatus.ACTIVE, pageable)
                .map(CommentResponse::from));
    }

    @Transactional
    public CommentResponse update(Long userId, String commentIdValue, String contentValue) {
        Comment comment = findComment(parseCommentId(commentIdValue));
        verifyOwner(comment, userId);
        comment.updateContent(normalizeContent(contentValue));
        commentRepository.flush();
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long userId, String commentIdValue) {
        Comment comment = findComment(parseCommentId(commentIdValue));
        verifyOwner(comment, userId);
        comment.delete(LocalDateTime.now());
    }

    private Video findVideo(long videoId) {
        return videoRepository.findByIdAndDeletedAtIsNull(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }

    private Comment findComment(long commentId) {
        return commentRepository.findByIdAndStatus(commentId, CommentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
    }

    private void verifyOwner(Comment comment, Long userId) {
        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.REQUIRED_FIELD_MISSING);
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_COMMENT_CONTENT);
        }
        return normalized;
    }

    private long parseVideoId(String value) {
        return parsePositiveLong(value, ErrorCode.INVALID_VIDEO_ID);
    }

    private long parseCommentId(String value) {
        return parsePositiveLong(value, ErrorCode.INVALID_COMMENT_ID);
    }

    private long parsePositiveLong(String value, ErrorCode errorCode) {
        try {
            long id = Long.parseLong(value);
            if (id < 1) throw new BusinessException(errorCode);
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(errorCode);
        }
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
