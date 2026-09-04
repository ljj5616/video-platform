package com.example.videoplatform.reaction.bookmark.repository;

import com.example.videoplatform.reaction.bookmark.entity.Bookmark;
import com.example.videoplatform.reaction.bookmark.entity.BookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {
}
