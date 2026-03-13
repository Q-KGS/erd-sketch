package com.erdsketch.pdf;

import com.erdsketch.auth.UserPrincipal;
import com.erdsketch.document.DocumentResponse;
import com.erdsketch.document.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents/{documentId}/export")
@RequiredArgsConstructor
public class PdfExportController {

    private final PdfExportService pdfExportService;
    private final DocumentService documentService;

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse document = documentService.get(documentId, principal.getId());
        String schemaJson = documentService.getSchema(documentId, principal.getId());
        byte[] pdf = pdfExportService.generatePdf(document.name(), document.name(), schemaJson);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"erdsketch-" + documentId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
