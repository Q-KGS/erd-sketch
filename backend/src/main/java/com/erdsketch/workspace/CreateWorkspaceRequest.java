package com.erdsketch.workspace;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateWorkspaceRequest(
    @NotBlank String name,
    @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String slug
) {}
