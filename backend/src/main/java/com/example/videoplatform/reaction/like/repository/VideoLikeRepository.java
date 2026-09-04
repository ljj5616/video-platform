package com.example.videoplatform.reaction.like.repository;

import com.example.videoplatform.reaction.like.entity.VideoLike;
import com.example.videoplatform.reaction.like.entity.VideoLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoLikeRepository extends JpaRepository<VideoLike, VideoLikeId> {
}
