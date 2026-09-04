package com.example.videoplatform.history.repository;

import com.example.videoplatform.history.entity.WatchHistory;
import com.example.videoplatform.history.entity.WatchHistoryId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, WatchHistoryId> {

    @EntityGraph(attributePaths = {"video", "video.uploader"})
    Page<WatchHistory> findByUser_IdOrderByLastWatchedAtDesc(Long userId, Pageable pageable);
}
