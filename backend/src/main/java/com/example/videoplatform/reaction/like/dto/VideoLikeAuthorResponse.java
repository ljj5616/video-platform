package com.example.videoplatform.reaction.like.dto;

import com.example.videoplatform.user.entity.User;

public record VideoLikeAuthorResponse(Long userId, String nickname) {

    public static VideoLikeAuthorResponse from(User user) {
        return new VideoLikeAuthorResponse(user.getId(), user.getNickname());
    }
}
