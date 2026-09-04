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
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    @EntityGraph(attributePaths = {"uploader", "category"})
    Optional<Video> findByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.id = :id and v.deletedAt is null")
    Optional<Video> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"uploader", "category"})
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

    @EntityGraph(attributePaths = {"uploader", "category"})
    @Query("""
            select v
            from Video v
            join v.uploader u
            where v.visibility = :visibility
              and v.status = :status
              and v.deletedAt is null
              and (:categoryId is null or v.category.id = :categoryId)
              and (
                    :keyword is null
                    or lower(v.title) like :keyword escape '\\'
                    or lower(v.description) like :keyword escape '\\'
                    or lower(u.nickname) like :keyword escape '\\'
              )
            """)
    Page<Video> filter(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("visibility") VideoVisibility visibility,
            @Param("status") VideoStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"uploader", "category"})
    Page<Video> findByCategory_IdAndVisibilityAndStatusAndDeletedAtIsNull(
            Long categoryId,
            VideoVisibility visibility,
            VideoStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "uploader")
    @Query(value = """
            select v
            from Video v
            left join WatchHistory watched
              on watched.video = v and watched.user.id = :userId
            left join WatchHistory categoryHistory
              on categoryHistory.video.category = v.category
             and categoryHistory.user.id = :userId
            where v.visibility = :visibility
              and v.status = :status
              and v.deletedAt is null
            group by v
            order by
              case when watched.id is null then 0 else 1 end,
              case when count(categoryHistory.id.userId) > 0 then 0 else 1 end,
              count(categoryHistory.id.userId) desc,
              v.viewCount desc,
              v.createdAt desc,
              v.id desc
            """, countQuery = """
            select count(v)
            from Video v
            where v.visibility = :visibility
              and v.status = :status
              and v.deletedAt is null
            """)
    Page<Video> findRecommendations(
            @Param("userId") Long userId,
            @Param("visibility") VideoVisibility visibility,
            @Param("status") VideoStatus status,
            Pageable pageable
    );
}
