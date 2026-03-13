package com.erdsketch.ddl;

import com.erdsketch.document.*;
import com.erdsketch.document.DocumentRepository;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DdlService {

    private final DocumentRepository documentRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final PostgreSqlDdlGenerator postgresGenerator;
    private final MySqlDdlGenerator mysqlGenerator;
    private final DdlParser ddlParser;

    public DdlGenerateResponse generate(UUID documentId, DdlGenerateRequest request, UUID userId) {
        ErdDocument doc = findAndCheck(documentId, userId);
        String snapshot = doc.getSchemaSnapshot();
        if (snapshot == null || snapshot.isBlank()) {
            return new DdlGenerateResponse("-- No schema defined yet", List.of());
        }

        try {
            Map<String, Object> schema = objectMapper.readValue(snapshot, new TypeReference<>() {});
            List<String> warnings = new ArrayList<>();
            String ddl = switch (request.dialect()) {
                case POSTGRESQL -> postgresGenerator.generate(schema, request.tableIds(), request.includeDrops(), warnings);
                case MYSQL -> mysqlGenerator.generate(schema, request.tableIds(), request.includeDrops(), warnings);
                default -> "-- DDL generation for " + request.dialect() + " is not yet implemented";
            };
            return new DdlGenerateResponse(ddl, warnings);
        } catch (Exception e) {
            return new DdlGenerateResponse("-- Error generating DDL: " + e.getMessage(), List.of());
        }
    }

    public DdlParseResponse parse(UUID documentId, DdlParseRequest request, UUID userId) {
        findAndCheck(documentId, userId);
        DdlParser.ParseResult result = ddlParser.parse(request.ddl());
        return new DdlParseResponse(result.tables(), result.relationships(), result.warnings());
    }

    private ErdDocument findAndCheck(UUID documentId, UUID userId) {
        ErdDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        if (!memberRepository.existsByWorkspaceIdAndUserId(doc.getProject().getWorkspace().getId(), userId)) {
            throw new AccessDeniedException("Access denied");
        }
        return doc;
    }
}
