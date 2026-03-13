package com.erdsketch.comment;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCommentRequest(
        @NotBlank String targetType,
        String targetId,
        @NotBlank String content,
        UUID parentId
) {
}
