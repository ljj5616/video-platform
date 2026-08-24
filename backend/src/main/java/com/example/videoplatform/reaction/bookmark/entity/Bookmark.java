package com.example.videoplatform.reaction.bookmark.entity;

import com.example.videoplatform.global.entity.BaseTimeEntity;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.video.entity.Video;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "bookmarks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseTimeEntity {

    @EmbeddedId
    private BookmarkId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("videoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
}
