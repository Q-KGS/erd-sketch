package com.erdsketch.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(UUID id, UUID projectId, String name, Instant createdAt, Instant updatedAt) {
    public static DocumentResponse from(ErdDocument d) {
        return new DocumentResponse(d.getId(), d.getProject().getId(), d.getName(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
