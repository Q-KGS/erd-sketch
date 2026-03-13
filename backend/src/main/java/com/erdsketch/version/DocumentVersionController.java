package com.erdsketch.version;

import com.erdsketch.auth.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentVersionController {

    private final DocumentVersionService versionService;

    @PostMapping("/api/v1/documents/{documentId}/versions")
    public ResponseEntity<DocumentVersionResponse> createVersion(
            @PathVariable UUID documentId,
            @RequestBody(required = false) CreateVersionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.createVersion(documentId, request, principal.getId()));
    }

    @GetMapping("/api/v1/documents/{documentId}/versions")
    public ResponseEntity<List<DocumentVersionResponse>> listVersions(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.listVersions(documentId, principal.getId()));
    }

    @GetMapping("/api/v1/documents/{documentId}/versions/{versionId}")
    public ResponseEntity<DocumentVersionResponse> getVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.getVersion(documentId, versionId, principal.getId()));
    }

    @PostMapping("/api/v1/documents/{documentId}/versions/{versionId}/restore")
    public ResponseEntity<DocumentVersionResponse> restoreVersion(
            @PathVariable UUID documentId,
            @PathVariable UUID versionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(versionService.restoreVersion(documentId, versionId, principal.getId()));
    }
}
