package com.example.videoplatform.video.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.videoplatform.category.entity.Category;
import com.example.videoplatform.category.repository.CategoryRepository;
import com.example.videoplatform.history.entity.WatchHistory;
import com.example.videoplatform.history.repository.WatchHistoryRepository;
import com.example.videoplatform.user.entity.User;
import com.example.videoplatform.user.repository.UserRepository;
import com.example.videoplatform.video.entity.Video;
import com.example.videoplatform.video.entity.VideoStatus;
import com.example.videoplatform.video.entity.VideoVisibility;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-that-is-at-least-32-bytes-long",
        "spring.datasource.url=jdbc:h2:mem:recommendation-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VideoRecommendationRepositoryTest {

    @Autowired VideoRepository videoRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired WatchHistoryRepository watchHistoryRepository;
    @Autowired UserRepository userRepository;

    @Test
    void prioritizesPreferredUnwatchedThenOtherUnwatchedThenWatchedVideos() {
        User user = userRepository.save(User.create(
                "recommendation@example.com", "password", "추천사용자", "추천 사용자", null));
        Category preferred = categoryRepository.save(category("선호 카테고리"));
        Category other = categoryRepository.save(category("기타 카테고리"));

        Video watchedPreferred = videoRepository.save(video(user, preferred, "이미 본 선호 영상", 10));
        Video unwatchedPreferred = videoRepository.save(video(user, preferred, "안 본 선호 영상", 1));
        Video unwatchedOther = videoRepository.save(video(user, other, "안 본 인기 영상", 20));
        watchHistoryRepository.save(new WatchHistory(user, watchedPreferred, LocalDateTime.now()));

        var result = videoRepository.findRecommendations(user.getId(), VideoVisibility.PUBLIC,
                VideoStatus.PUBLISHED, PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Video::getId).containsExactly(
                unwatchedPreferred.getId(), unwatchedOther.getId(), watchedPreferred.getId());
    }

    @Test
    void filtersByKeywordAndCategoryAndSortsByPopularity() {
        User user = userRepository.save(User.create(
                "filter@example.com", "password", "필터사용자", "필터 사용자", null));
        Category category = categoryRepository.save(category("필터 카테고리"));
        Video lessPopular = videoRepository.save(video(user, category, "스프링 기초", 1));
        Video morePopular = videoRepository.save(video(user, category, "스프링 심화", 20));
        videoRepository.save(video(user, category, "자바 기초", 100));

        var result = videoRepository.filter("%스프링%", category.getId(), VideoVisibility.PUBLIC,
                VideoStatus.PUBLISHED, PageRequest.of(0, 20,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Order.desc("viewCount"),
                                org.springframework.data.domain.Sort.Order.desc("id"))));

        assertThat(result.getContent()).extracting(Video::getId)
                .containsExactly(morePopular.getId(), lessPopular.getId());
    }

    private Category category(String name) {
        Category category = BeanUtils.instantiateClass(Category.class);
        ReflectionTestUtils.setField(category, "name", name);
        return category;
    }

    private Video video(User uploader, Category category, String title, int viewCount) {
        Video video = new Video(uploader, category, title, null, VideoVisibility.PUBLIC, null, null);
        video.completeProcessing("https://example.com/video.mp4", 60);
        for (int count = 0; count < viewCount; count++) {
            video.increaseViewCount();
        }
        return video;
    }
}
