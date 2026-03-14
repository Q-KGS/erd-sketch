package com.erdsketch.dbml;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/dbml")
public class DbmlController {

    private final DbmlService service;

    @PostMapping("/parse")
    public ResponseEntity<DbmlParseResult> parse(@Valid @RequestBody DbmlParseRequest request) {
        return ResponseEntity.ok(service.parse(request.dbml()));
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(@Valid @RequestBody DbmlGenerateRequest request) {
        String dbml = service.generate(request.tables(), request.relationships());
        return ResponseEntity.ok(Map.of("dbml", dbml));
    }
}
