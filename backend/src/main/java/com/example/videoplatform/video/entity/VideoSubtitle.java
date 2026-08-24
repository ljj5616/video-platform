package com.example.videoplatform.video.entity;

import com.example.videoplatform.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "video_subtitles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VideoSubtitle extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false, length = 10)
    private String languageCode;

    @Column(nullable = false, length = 50)
    private String label;

    @Column(nullable = false, length = 1000)
    private String subtitleUrl;

    @Column(nullable = false)
    private boolean defaultSubtitle;
}
