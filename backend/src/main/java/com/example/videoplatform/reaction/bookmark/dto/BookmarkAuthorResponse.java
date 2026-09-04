package com.example.videoplatform.reaction.bookmark.dto;

import com.example.videoplatform.user.entity.User;

public record BookmarkAuthorResponse(Long userId, String nickname) {

    public static BookmarkAuthorResponse from(User user) {
        return new BookmarkAuthorResponse(user.getId(), user.getNickname());
    }
}
