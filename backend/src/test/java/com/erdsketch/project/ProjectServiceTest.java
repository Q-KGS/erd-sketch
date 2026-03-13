package com.erdsketch.project;

import com.erdsketch.support.BaseIntegrationTest;
import com.erdsketch.support.MockUserSupport;
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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectServiceTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProjectService projectService;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired ProjectRepository projectRepository;

    User member;
    User outsider;
    Workspace workspace;

    @BeforeEach
    void setUp() {
        member = userRepository.saveAndFlush(TestDataFactory.user("member"));
        outsider = userRepository.saveAndFlush(TestDataFactory.user("outsider"));
        workspace = workspaceRepository.saveAndFlush(TestDataFactory.workspace(member));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(workspace, member, WorkspaceRole.OWNER));
    }

    // ───── B-PRJ-01: 멤버가 프로젝트 생성 ─────
    @Test
    void B_PRJ_01_멤버가_프로젝트_생성() {
        ProjectResponse resp = projectService.create(workspace.getId(),
                new CreateProjectRequest("My Project", "desc", DbType.POSTGRESQL),
                member.getId());

        assertThat(resp.name()).isEqualTo("My Project");
        assertThat(resp.targetDbType()).isEqualTo(DbType.POSTGRESQL);
    }

    // ───── B-PRJ-02: 비멤버가 프로젝트 생성 시도 → 403 ─────
    @Test
    void B_PRJ_02_비멤버_프로젝트_생성_거부() {
        assertThrows(AccessDeniedException.class, () ->
                projectService.create(workspace.getId(),
                        new CreateProjectRequest("Hack", null, DbType.MYSQL),
                        outsider.getId()));
    }

    // ───── B-PRJ-03: 유효하지 않은 targetDbType → 400 ─────
    @Test
    void B_PRJ_03_유효하지_않은_dbType() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/" + workspace.getId() + "/projects")
                        .with(MockUserSupport.mockUser(member.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"targetDbType\":\"INVALID_DB\"}"))
                .andExpect(status().isBadRequest());
    }

    // ───── B-PRJ-04: 프로젝트 목록 조회 ─────
    @Test
    void B_PRJ_04_프로젝트_목록_조회() {
        projectService.create(workspace.getId(),
                new CreateProjectRequest("P1", null, DbType.MYSQL), member.getId());
        projectService.create(workspace.getId(),
                new CreateProjectRequest("P2", null, DbType.POSTGRESQL), member.getId());

        var list = projectService.list(workspace.getId(), member.getId());
        assertThat(list).hasSize(2);
        assertThat(list).extracting(ProjectResponse::name).containsExactlyInAnyOrder("P1", "P2");
    }

    // ───── B-PRJ-05: 프로젝트 수정 ─────
    @Test
    void B_PRJ_05_프로젝트_수정() {
        ProjectResponse created = projectService.create(workspace.getId(),
                new CreateProjectRequest("Old Name", null, DbType.POSTGRESQL), member.getId());

        ProjectResponse updated = projectService.update(created.id(),
                new UpdateProjectRequest("New Name", null, DbType.MYSQL), member.getId());

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.targetDbType()).isEqualTo(DbType.MYSQL);
    }

    // ───── B-PRJ-06: 프로젝트 삭제 후 조회 → 예외 ─────
    @Test
    void B_PRJ_06_프로젝트_삭제_후_조회() {
        ProjectResponse created = projectService.create(workspace.getId(),
                new CreateProjectRequest("Delete Me", null, DbType.ORACLE), member.getId());

        projectService.delete(created.id(), member.getId());

        assertThrows(IllegalArgumentException.class,
                () -> projectService.get(created.id(), member.getId()));
    }

    // ───── 다른 워크스페이스 프로젝트는 목록에 미포함 ─────
    @Test
    void 다른_워크스페이스_프로젝트는_목록에_미포함() {
        User owner2 = userRepository.saveAndFlush(TestDataFactory.user("owner2"));
        Workspace ws2 = workspaceRepository.saveAndFlush(TestDataFactory.workspace(owner2));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(ws2, owner2, WorkspaceRole.OWNER));

        projectService.create(workspace.getId(),
                new CreateProjectRequest("WS1 Project", null, DbType.POSTGRESQL), member.getId());
        projectService.create(ws2.getId(),
                new CreateProjectRequest("WS2 Project", null, DbType.MYSQL), owner2.getId());

        var list = projectService.list(workspace.getId(), member.getId());
        assertThat(list).hasSize(1);
        assertThat(list.getFirst().name()).isEqualTo("WS1 Project");
    }
}
