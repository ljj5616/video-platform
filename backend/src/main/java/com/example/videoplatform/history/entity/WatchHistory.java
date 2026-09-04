package com.example.videoplatform.history.entity;

import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.video.entity.Video;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "watch_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchHistory {

    @EmbeddedId
    private WatchHistoryId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("videoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private Integer positionSeconds = 0;

    @Column(nullable = false)
    private Integer watchedSeconds = 0;

    @Column(nullable = false)
    private boolean viewCounted;

    @Column(nullable = false, updatable = false)
    private LocalDateTime firstWatchedAt;

    @Column(nullable = false)
    private LocalDateTime lastWatchedAt;

    private LocalDateTime completedAt;

    public WatchHistory(User user, Video video, LocalDateTime watchedAt) {
        this.id = new WatchHistoryId(user.getId(), video.getId());
        this.user = user;
        this.video = video;
        this.firstWatchedAt = watchedAt;
        this.lastWatchedAt = watchedAt;
        this.viewCounted = true;
    }

    public boolean countView(LocalDateTime watchedAt) {
        this.lastWatchedAt = watchedAt;
        if (viewCounted) {
            return false;
        }
        this.viewCounted = true;
        return true;
    }
}
