package com.example.videoplatform.video.repository;

import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    @EntityGraph(attributePaths = {"uploader", "category"})
    Optional<Video> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "uploader")
    @Query("""
            select v
            from Video v
            join v.uploader u
            where v.visibility = :visibility
              and v.status = :status
              and v.deletedAt is null
              and (
                    lower(v.title) like :keyword escape '\\'
                    or lower(v.description) like :keyword escape '\\'
                    or lower(u.nickname) like :keyword escape '\\'
              )
            """)
    Page<Video> search(
            @Param("keyword") String keyword,
            @Param("visibility") VideoVisibility visibility,
            @Param("status") VideoStatus status,
            Pageable pageable
    );
}
