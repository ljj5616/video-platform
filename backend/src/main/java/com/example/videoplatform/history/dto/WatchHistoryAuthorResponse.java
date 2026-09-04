package com.example.videoplatform.history.dto;

import com.example.videoplatform.user.entity.User;

public record WatchHistoryAuthorResponse(Long userId, String nickname) {

    public static WatchHistoryAuthorResponse from(User user) {
        return new WatchHistoryAuthorResponse(user.getId(), user.getNickname());
    }
}
