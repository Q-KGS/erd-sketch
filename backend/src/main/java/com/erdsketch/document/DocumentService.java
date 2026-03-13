package com.erdsketch.document;

import com.erdsketch.common.exception.ResourceNotFoundException;
import com.erdsketch.project.Project;
import com.erdsketch.project.ProjectRepository;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Transactional
    public DocumentResponse create(UUID projectId, CreateDocumentRequest request, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
        checkAccess(project.getWorkspace().getId(), userId);
        ErdDocument doc = ErdDocument.builder()
                .project(project)
                .name(request.name())
                .build();
        documentRepository.save(doc);
        return DocumentResponse.from(doc);
    }

    public List<DocumentResponse> list(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
        checkAccess(project.getWorkspace().getId(), userId);
        return documentRepository.findByProjectId(projectId)
                .stream().map(DocumentResponse::from).toList();
    }

    public DocumentResponse get(UUID documentId, UUID userId) {
        ErdDocument doc = findAndCheck(documentId, userId);
        return DocumentResponse.from(doc);
    }

    @Transactional
    public void delete(UUID documentId, UUID userId) {
        ErdDocument doc = findAndCheck(documentId, userId);
        documentRepository.delete(doc);
    }

    public String getSchema(UUID documentId, UUID userId) {
        ErdDocument doc = findAndCheck(documentId, userId);
        return doc.getSchemaSnapshot();
    }

    @Transactional
    public void updateYjsState(UUID documentId, byte[] yjsState, String schemaSnapshot) {
        ErdDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        doc.setYjsState(yjsState);
        doc.setSchemaSnapshot(schemaSnapshot);
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
