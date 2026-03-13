package com.erdsketch.jdbc;

import jakarta.validation.constraints.NotBlank;

public record JdbcConnectionRequest(
        @NotBlank String host,
        int port,
        @NotBlank String database,
        @NotBlank String username,
        @NotBlank String password,
        String dbType
) {}
