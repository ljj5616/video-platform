package com.example.videoplatform.reaction.like.repository;

import com.example.videoplatform.reaction.like.entity.VideoLike;
import com.example.videoplatform.reaction.like.entity.VideoLikeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLikeId> {

    @EntityGraph(attributePaths = {"video", "video.uploader"})
    Page<VideoLike> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
