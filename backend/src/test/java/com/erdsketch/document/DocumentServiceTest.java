package com.erdsketch.document;

import com.erdsketch.project.Project;
import com.erdsketch.project.ProjectRepository;
import com.erdsketch.common.exception.ResourceNotFoundException;
import com.erdsketch.support.BaseIntegrationTest;
import com.erdsketch.support.TestDataFactory;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import com.erdsketch.workspace.Workspace;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import com.erdsketch.workspace.WorkspaceRepository;
import com.erdsketch.workspace.WorkspaceRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentServiceTest extends BaseIntegrationTest {

    @Autowired DocumentService documentService;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired ProjectRepository projectRepository;

    User member;
    User outsider;
    Project project;

    @BeforeEach
    void setUp() {
        member = userRepository.saveAndFlush(TestDataFactory.user("member"));
        outsider = userRepository.saveAndFlush(TestDataFactory.user("outsider"));

        Workspace workspace = workspaceRepository.saveAndFlush(TestDataFactory.workspace(member));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(workspace, member, WorkspaceRole.OWNER));

        project = projectRepository.saveAndFlush(TestDataFactory.project(workspace, member));
    }

    // ───── B-DOC-01: 문서 생성 ─────
    @Test
    void B_DOC_01_문서_생성() {
        DocumentResponse resp = documentService.create(project.getId(),
                new CreateDocumentRequest("My ERD"), member.getId());

        assertThat(resp.name()).isEqualTo("My ERD");
        assertThat(resp.projectId()).isEqualTo(project.getId());
    }

    // ───── B-DOC-02: 다른 워크스페이스 사용자 조회 → 403 ─────
    @Test
    void B_DOC_02_비멤버_문서_조회_거부() {
        DocumentResponse doc = documentService.create(project.getId(),
                new CreateDocumentRequest("Secret ERD"), member.getId());

        assertThrows(AccessDeniedException.class,
                () -> documentService.get(doc.id(), outsider.getId()));
    }

    // ───── B-DOC-03: schema_snapshot null인 문서 schema 조회 → null ─────
    @Test
    void B_DOC_03_null_스냅샷_조회() {
        DocumentResponse doc = documentService.create(project.getId(),
                new CreateDocumentRequest("Empty ERD"), member.getId());

        String schema = documentService.getSchema(doc.id(), member.getId());
        assertThat(schema).isNull();
    }

    // ───── B-DOC-04: 문서 삭제 후 조회 → 예외 ─────
    @Test
    void B_DOC_04_문서_삭제_후_조회_예외() {
        DocumentResponse doc = documentService.create(project.getId(),
                new CreateDocumentRequest("Delete Me"), member.getId());

        documentService.delete(doc.id(), member.getId());

        assertThrows(ResourceNotFoundException.class,
                () -> documentService.get(doc.id(), member.getId()));
    }

    // ───── 추가: 문서 목록 조회 ─────
    @Test
    void 프로젝트_내_문서_목록_조회() {
        documentService.create(project.getId(), new CreateDocumentRequest("ERD 1"), member.getId());
        documentService.create(project.getId(), new CreateDocumentRequest("ERD 2"), member.getId());

        var list = documentService.list(project.getId(), member.getId());
        assertThat(list).hasSize(2);
    }
}
