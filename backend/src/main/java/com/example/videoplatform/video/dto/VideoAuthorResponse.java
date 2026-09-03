package com.example.videoplatform.video.dto;

import com.example.videoplatform.user.entity.User;

public record VideoAuthorResponse(
        Long userId,
        String nickname,
        String profileImageUrl
) {
    public static VideoAuthorResponse from(User user) {
        return new VideoAuthorResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }
}
