package com.erdsketch.document;

import com.erdsketch.auth.UserPrincipal;
import com.erdsketch.ddl.DdlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DdlService ddlService;

    @PostMapping("/api/v1/projects/{projectId}/documents")
    public ResponseEntity<DocumentResponse> create(@PathVariable UUID projectId,
                                                    @Valid @RequestBody CreateDocumentRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.create(projectId, request, principal.getId()));
    }

    @GetMapping("/api/v1/projects/{projectId}/documents")
    public ResponseEntity<List<DocumentResponse>> list(@PathVariable UUID projectId,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.list(projectId, principal.getId()));
    }

    @GetMapping("/api/v1/documents/{documentId}")
    public ResponseEntity<DocumentResponse> get(@PathVariable UUID documentId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.get(documentId, principal.getId()));
    }

    @DeleteMapping("/api/v1/documents/{documentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID documentId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        documentService.delete(documentId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/documents/{documentId}/schema")
    public ResponseEntity<String> getSchema(@PathVariable UUID documentId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getSchema(documentId, principal.getId()));
    }

    @PostMapping("/api/v1/documents/{documentId}/ddl/generate")
    public ResponseEntity<DdlGenerateResponse> generateDdl(@PathVariable UUID documentId,
                                                             @RequestBody DdlGenerateRequest request,
                                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ddlService.generate(documentId, request, principal.getId()));
    }

    @PostMapping("/api/v1/documents/{documentId}/ddl/parse")
    public ResponseEntity<DdlParseResponse> parseDdl(@PathVariable UUID documentId,
                                                       @RequestBody DdlParseRequest request,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ddlService.parse(documentId, request, principal.getId()));
    }
}
