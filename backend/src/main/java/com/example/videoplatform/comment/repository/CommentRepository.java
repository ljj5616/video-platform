package com.example.videoplatform.comment.repository;

import com.example.videoplatform.comment.entity.Comment;
import com.example.videoplatform.comment.entity.CommentStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Comment> findByVideoIdAndStatus(Long videoId, CommentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "video"})
    Optional<Comment> findByIdAndStatus(Long id, CommentStatus status);
}
