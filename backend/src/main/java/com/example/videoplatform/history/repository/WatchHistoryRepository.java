package com.example.videoplatform.history.repository;

import com.example.videoplatform.history.entity.WatchHistory;
import com.example.videoplatform.history.entity.WatchHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchHistoryRepository extends JpaRepository<WatchHistory, WatchHistoryId> {
}
