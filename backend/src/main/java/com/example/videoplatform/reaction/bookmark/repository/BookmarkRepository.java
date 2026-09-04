package com.example.videoplatform.reaction.bookmark.repository;

import com.example.videoplatform.reaction.bookmark.entity.Bookmark;
import com.example.videoplatform.reaction.bookmark.entity.BookmarkId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    @EntityGraph(attributePaths = {"video", "video.uploader"})
    Page<Bookmark> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
