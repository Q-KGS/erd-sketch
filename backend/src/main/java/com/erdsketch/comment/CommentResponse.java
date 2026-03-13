package com.erdsketch.comment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID documentId,
        UUID authorId,
        String authorDisplayName,
        String targetType,
        String targetId,
        String content,
        boolean resolved,
        UUID parentId,
        List<CommentResponse> replies,
        Instant createdAt,
        Instant updatedAt
) {
    public static CommentResponse from(Comment c, List<CommentResponse> replies) {
        return new CommentResponse(
                c.getId(),
                c.getDocument().getId(),
                c.getAuthor().getId(),
                c.getAuthor().getDisplayName(),
                c.getTargetType().name(),
                c.getTargetId(),
                c.getContent(),
                c.isResolved(),
                c.getParentId(),
                replies,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
