package com.erdsketch.version;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        UUID documentId,
        int versionNumber,
        String label,
        UUID createdBy,
        Instant createdAt
) {
    public static DocumentVersionResponse from(DocumentVersion v) {
        return new DocumentVersionResponse(
                v.getId(),
                v.getDocument().getId(),
                v.getVersionNumber(),
                v.getLabel(),
                v.getCreatedBy().getId(),
                v.getCreatedAt()
        );
    }
}
