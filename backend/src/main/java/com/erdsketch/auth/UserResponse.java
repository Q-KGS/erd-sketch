package com.erdsketch.auth;

import com.erdsketch.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String displayName, String avatarUrl, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl(), user.getCreatedAt());
    }
}
