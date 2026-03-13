package com.erdsketch.version;

import com.erdsketch.common.exception.ResourceNotFoundException;
import com.erdsketch.document.DocumentRepository;
import com.erdsketch.document.ErdDocument;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentVersionService {

    private final DocumentVersionRepository versionRepository;
    private final DocumentRepository documentRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public DocumentVersionResponse createVersion(UUID documentId, CreateVersionRequest request, UUID userId) {
        ErdDocument doc = findAndCheck(documentId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        int nextVersionNumber = versionRepository
                .findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        DocumentVersion version = DocumentVersion.builder()
                .document(doc)
                .versionNumber(nextVersionNumber)
                .yjsState(doc.getYjsState())
                .schemaSnapshot(doc.getSchemaSnapshot())
                .label(request != null ? request.label() : null)
                .createdBy(user)
                .build();

        return DocumentVersionResponse.from(versionRepository.save(version));
    }

    public List<DocumentVersionResponse> listVersions(UUID documentId, UUID userId) {
        findAndCheck(documentId, userId);
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(DocumentVersionResponse::from)
                .toList();
    }

    public DocumentVersionResponse getVersion(UUID documentId, UUID versionId, UUID userId) {
        findAndCheck(documentId, userId);
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));
        if (!version.getDocument().getId().equals(documentId)) {
            throw new IllegalArgumentException("Version does not belong to this document");
        }
        return DocumentVersionResponse.from(version);
    }

    @Transactional
    public DocumentVersionResponse restoreVersion(UUID documentId, UUID versionId, UUID userId) {
        ErdDocument doc = findAndCheck(documentId, userId);
        DocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Version", versionId));
        if (!version.getDocument().getId().equals(documentId)) {
            throw new IllegalArgumentException("Version does not belong to this document");
        }

        doc.setYjsState(version.getYjsState());
        doc.setSchemaSnapshot(version.getSchemaSnapshot());
        documentRepository.save(doc);

        return DocumentVersionResponse.from(version);
    }

    ErdDocument findAndCheck(UUID documentId, UUID userId) {
        ErdDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        checkAccess(doc.getProject().getWorkspace().getId(), userId);
        return doc;
    }

    private void checkAccess(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
