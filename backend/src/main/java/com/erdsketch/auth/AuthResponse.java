package com.erdsketch.auth;

public record AuthResponse(UserResponse user, TokenResponse tokens) {}
