package com.example.videoplatform.comment.dto;

import com.example.videoplatform.user.entity.User;

public record CommentAuthorResponse(Long userId, String nickname, String profileImageUrl) {

    public static CommentAuthorResponse from(User user) {
        return new CommentAuthorResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }
}
