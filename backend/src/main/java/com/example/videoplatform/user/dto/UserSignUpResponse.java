package com.example.videoplatform.user.dto;

import com.example.videoplatform.user.entity.User;
import java.time.LocalDateTime;

public record UserSignUpResponse(
        Long id,
        String email,
        String nickname,
        String name,
        LocalDateTime createdAt
) {
    public static UserSignUpResponse from(User user) {
        return new UserSignUpResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getCreatedAt()
        );
    }
}
