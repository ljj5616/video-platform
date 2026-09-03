package com.example.videoplatform.video.entity;

import com.example.videoplatform.category.entity.Category;
import com.example.videoplatform.global.entity.BaseTimeEntity;
import com.example.videoplatform.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "videos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Video extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoVisibility visibility = VideoVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VideoStatus status = VideoStatus.UPLOADING;

    @Column(length = 1000)
    private String videoUrl;

    @Column(length = 1000)
    private String thumbnailUrl;

    private Integer durationSeconds;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Long likeCount = 0L;

    private LocalDateTime publishedAt;

    private LocalDateTime deletedAt;

    public Video(User uploader, Category category, String title, String description,
                 VideoVisibility visibility, String videoUrl, String thumbnailUrl) {
        this.uploader = uploader;
        this.category = category;
        this.title = title;
        this.description = description;
        this.visibility = visibility;
        this.status = VideoStatus.PROCESSING;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void completeProcessing(String videoUrl, Integer durationSeconds) {
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds;
        this.status = VideoStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    public void failProcessing() {
        this.status = VideoStatus.FAILED;
    }
}
