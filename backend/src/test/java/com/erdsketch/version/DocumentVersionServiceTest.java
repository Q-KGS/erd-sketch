package com.erdsketch.version;

import com.erdsketch.collaboration.SnapshotScheduler;
import com.erdsketch.collaboration.YjsWebSocketHandler;
import com.erdsketch.common.exception.ResourceNotFoundException;
import com.erdsketch.document.DocumentRepository;
import com.erdsketch.document.DocumentService;
import com.erdsketch.document.ErdDocument;
import com.erdsketch.project.DbType;
import com.erdsketch.project.Project;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import com.erdsketch.workspace.Workspace;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentVersionServiceTest {

    @Mock DocumentVersionRepository versionRepository;
    @Mock DocumentRepository documentRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @Mock UserRepository userRepository;
    @InjectMocks DocumentVersionService versionService;

    UUID userId;
    UUID workspaceId;
    UUID documentId;
    User user;
    ErdDocument document;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .displayName("Test User")
                .passwordHash("hash")
                .build();

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test Workspace")
                .slug("test-ws")
                .owner(user)
                .build();

        Project project = Project.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .name("Test Project")
                .targetDbType(DbType.POSTGRESQL)
                .createdBy(user)
                .build();

        document = ErdDocument.builder()
                .id(documentId)
                .project(project)
                .name("Test ERD")
                .schemaSnapshot("{\"tables\":{}}")
                .yjsState(new byte[]{1, 2, 3})
                .build();
    }

    // ───── B-VER-01: createVersion → versionNumber = 1 ─────
    @Test
    void B_VER_01_첫_번째_버전_생성_시_버전_번호는_1() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)).willReturn(Optional.empty());
        given(versionRepository.save(any(DocumentVersion.class))).willAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v = DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .document(v.getDocument())
                    .versionNumber(v.getVersionNumber())
                    .label(v.getLabel())
                    .yjsState(v.getYjsState())
                    .schemaSnapshot(v.getSchemaSnapshot())
                    .createdBy(v.getCreatedBy())
                    .build();
            return v;
        });

        // when
        DocumentVersionResponse resp = versionService.createVersion(documentId, new CreateVersionRequest("v1"), userId);

        // then
        assertThat(resp.versionNumber()).isEqualTo(1);
        assertThat(resp.label()).isEqualTo("v1");
    }

    // ───── B-VER-02: 두 번 생성 → 1, 2 ─────
    @Test
    void B_VER_02_두_번_생성_시_버전_번호는_1_2() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        DocumentVersion existingVersion = DocumentVersion.builder()
                .id(UUID.randomUUID())
                .document(document)
                .versionNumber(1)
                .createdBy(user)
                .build();
        given(versionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)).willReturn(Optional.of(existingVersion));
        given(versionRepository.save(any(DocumentVersion.class))).willAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            return DocumentVersion.builder()
                    .id(UUID.randomUUID())
                    .document(v.getDocument())
                    .versionNumber(v.getVersionNumber())
                    .label(v.getLabel())
                    .yjsState(v.getYjsState())
                    .schemaSnapshot(v.getSchemaSnapshot())
                    .createdBy(v.getCreatedBy())
                    .build();
        });

        // when
        DocumentVersionResponse resp = versionService.createVersion(documentId, new CreateVersionRequest("v2"), userId);

        // then
        assertThat(resp.versionNumber()).isEqualTo(2);
    }

    // ───── B-VER-03: listVersions → 내림차순 ─────
    @Test
    void B_VER_03_버전_목록_내림차순_조회() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);

        DocumentVersion v2 = DocumentVersion.builder()
                .id(UUID.randomUUID()).document(document).versionNumber(2).createdBy(user).build();
        DocumentVersion v1 = DocumentVersion.builder()
                .id(UUID.randomUUID()).document(document).versionNumber(1).createdBy(user).build();

        given(versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId)).willReturn(List.of(v2, v1));

        // when
        List<DocumentVersionResponse> list = versionService.listVersions(documentId, userId);

        // then
        assertThat(list).hasSize(2);
        assertThat(list.get(0).versionNumber()).isEqualTo(2);
        assertThat(list.get(1).versionNumber()).isEqualTo(1);
    }

    // ───── B-VER-04: getVersion → schema_snapshot 포함 ─────
    @Test
    void B_VER_04_버전_조회_시_스키마_스냅샷_포함() {
        // given
        UUID versionId = UUID.randomUUID();
        DocumentVersion version = DocumentVersion.builder()
                .id(versionId)
                .document(document)
                .versionNumber(1)
                .schemaSnapshot("{\"tables\":{}}")
                .createdBy(user)
                .build();

        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(versionRepository.findById(versionId)).willReturn(Optional.of(version));

        // when
        DocumentVersionResponse resp = versionService.getVersion(documentId, versionId, userId);

        // then
        assertThat(resp.id()).isEqualTo(versionId);
        assertThat(resp.versionNumber()).isEqualTo(1);
    }

    // ───── B-VER-05: restoreVersion → doc의 yjsState, schemaSnapshot 교체 ─────
    @Test
    void B_VER_05_버전_복원_시_문서_상태_교체() {
        // given
        UUID versionId = UUID.randomUUID();
        byte[] versionState = {10, 20, 30};
        String versionSnapshot = "{\"tables\":{\"restored\":{}}}";

        DocumentVersion version = DocumentVersion.builder()
                .id(versionId)
                .document(document)
                .versionNumber(1)
                .yjsState(versionState)
                .schemaSnapshot(versionSnapshot)
                .createdBy(user)
                .build();

        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(versionRepository.findById(versionId)).willReturn(Optional.of(version));
        given(documentRepository.save(any(ErdDocument.class))).willReturn(document);

        // when
        versionService.restoreVersion(documentId, versionId, userId);

        // then
        assertThat(document.getYjsState()).isEqualTo(versionState);
        assertThat(document.getSchemaSnapshot()).isEqualTo(versionSnapshot);
        verify(documentRepository).save(document);
    }

    // ───── B-VER-06: SnapshotScheduler 동작 검증 ─────
    @Test
    void B_VER_06_SnapshotScheduler_직접_호출_시_문서_저장() {
        // given
        YjsWebSocketHandler wsHandler = mock(YjsWebSocketHandler.class);
        DocumentService documentService = mock(DocumentService.class);
        SnapshotScheduler scheduler = new SnapshotScheduler(wsHandler, documentService);

        String docId = UUID.randomUUID().toString();
        byte[] state = {1, 2, 3};
        given(wsHandler.drainPendingStates()).willReturn(Map.of(docId, state));

        // when
        scheduler.saveSnapshots();

        // then
        verify(documentService).updateYjsState(UUID.fromString(docId), state, null);
    }

    // ───── B-VER-07: 다른 문서 → AccessDeniedException ─────
    @Test
    void B_VER_07_비멤버_접근_시_AccessDeniedException() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> versionService.listVersions(documentId, userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ───── B-VER-08: 존재하지 않는 버전 → ResourceNotFoundException ─────
    @Test
    void B_VER_08_존재하지_않는_버전_조회_시_ResourceNotFoundException() {
        // given
        UUID nonExistentVersionId = UUID.randomUUID();
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(versionRepository.findById(nonExistentVersionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> versionService.getVersion(documentId, nonExistentVersionId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
