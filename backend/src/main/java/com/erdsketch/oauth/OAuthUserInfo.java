package com.erdsketch.oauth;

public record OAuthUserInfo(
        String provider,
        String subject,
        String email,
        String displayName,
        String avatarUrl
) {}
