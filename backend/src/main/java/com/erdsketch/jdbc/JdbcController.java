package com.erdsketch.jdbc;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jdbc")
public class JdbcController {

    private final JdbcSchemaExtractor extractor;

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testConnection(
            @Valid @RequestBody JdbcConnectionRequest request) {
        extractor.extractSchema(request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/extract")
    public ResponseEntity<List<Map<String, Object>>> extract(
            @Valid @RequestBody JdbcConnectionRequest request) {
        return ResponseEntity.ok(extractor.extractSchema(request));
    }
}
