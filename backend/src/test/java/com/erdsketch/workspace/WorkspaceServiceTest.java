package com.erdsketch.workspace;

import com.erdsketch.common.exception.DuplicateResourceException;
import com.erdsketch.common.exception.ResourceNotFoundException;
import com.erdsketch.support.BaseIntegrationTest;
import com.erdsketch.support.MockUserSupport;
import com.erdsketch.support.TestDataFactory;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WorkspaceServiceTest extends BaseIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired WorkspaceService workspaceService;
    @Autowired UserRepository userRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;

    User owner;
    User viewer;
    User admin;
    User outsider;
    Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = userRepository.saveAndFlush(TestDataFactory.user("owner"));
        viewer = userRepository.saveAndFlush(TestDataFactory.user("viewer"));
        admin = userRepository.saveAndFlush(TestDataFactory.user("admin"));
        outsider = userRepository.saveAndFlush(TestDataFactory.user("outsider"));

        workspace = workspaceRepository.saveAndFlush(TestDataFactory.workspace(owner));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(workspace, owner, WorkspaceRole.OWNER));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(workspace, viewer, WorkspaceRole.VIEWER));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(workspace, admin, WorkspaceRole.ADMIN));
    }

    // ───── B-WS-01: 워크스페이스 생성 시 OWNER 자동 추가 ─────
    @Test
    void B_WS_01_워크스페이스_생성시_OWNER_자동추가() {
        WorkspaceResponse resp = workspaceService.create(
                new CreateWorkspaceRequest("My WS", "my-ws-test"), owner.getId());

        Workspace created = workspaceRepository.findBySlug("my-ws-test").orElseThrow();
        assertThat(memberRepository.findByWorkspaceIdAndUserId(created.getId(), owner.getId()))
                .isPresent()
                .get()
                .extracting(WorkspaceMember::getRole)
                .isEqualTo(WorkspaceRole.OWNER);
    }

    // ───── B-WS-02: 중복 슬러그 생성 ─────
    @Test
    void B_WS_02_중복_슬러그_생성_예외() {
        String slug = workspace.getSlug();
        assertThrows(DuplicateResourceException.class,
                () -> workspaceService.create(new CreateWorkspaceRequest("Dup", slug), owner.getId()));
    }

    // ───── B-WS-03: 비멤버 사용자 조회 → 403 ─────
    @Test
    void B_WS_03_비멤버_조회_거부() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces/" + workspace.getId())
                        .with(MockUserSupport.mockUser(outsider.getId())))
                .andExpect(status().isForbidden());
    }

    // ───── B-WS-04: VIEWER가 PATCH 요청 → 403 ─────
    @Test
    void B_WS_04_VIEWER_수정_거부() throws Exception {
        mockMvc.perform(patch("/api/v1/workspaces/" + workspace.getId())
                        .with(MockUserSupport.mockUser(viewer.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New Name\"}"))
                .andExpect(status().isForbidden());
    }

    // ───── B-WS-05: OWNER가 아닌 사용자 DELETE → 403 ─────
    @Test
    void B_WS_05_비OWNER_삭제_거부() throws Exception {
        mockMvc.perform(delete("/api/v1/workspaces/" + workspace.getId())
                        .with(MockUserSupport.mockUser(admin.getId())))
                .andExpect(status().isForbidden());
    }

    // ───── B-WS-06: 이메일로 멤버 초대 ─────
    @Test
    void B_WS_06_이메일로_멤버_초대() {
        WorkspaceMemberResponse resp = workspaceService.inviteMember(
                workspace.getId(),
                new InviteMemberRequest(outsider.getEmail(), WorkspaceRole.MEMBER),
                owner.getId());

        assertThat(resp.role()).isEqualTo(WorkspaceRole.MEMBER);
        assertThat(memberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), outsider.getId())).isTrue();
    }

    // ───── B-WS-07: 없는 이메일로 초대 → 예외 ─────
    @Test
    void B_WS_07_없는_이메일_초대_예외() {
        assertThrows(ResourceNotFoundException.class, () ->
                workspaceService.inviteMember(
                        workspace.getId(),
                        new InviteMemberRequest("nobody@nowhere.com", WorkspaceRole.MEMBER),
                        owner.getId()));
    }

    // ───── B-WS-08: MEMBER가 초대 시도 → 403 ─────
    @Test
    void B_WS_08_MEMBER_초대_거부() {
        User member = userRepository.saveAndFlush(TestDataFactory.user("mbr"));
        memberRepository.saveAndFlush(TestDataFactory.workspaceMember(workspace, member, WorkspaceRole.MEMBER));

        assertThrows(org.springframework.security.access.AccessDeniedException.class, () ->
                workspaceService.inviteMember(
                        workspace.getId(),
                        new InviteMemberRequest(outsider.getEmail(), WorkspaceRole.VIEWER),
                        member.getId()));
    }

    // ───── B-WS-09: 멤버 역할 변경 ─────
    @Test
    void B_WS_09_멤버_역할_변경() {
        WorkspaceMemberResponse resp = workspaceService.updateMemberRole(
                workspace.getId(), admin.getId(), WorkspaceRole.MEMBER, owner.getId());

        assertThat(resp.role()).isEqualTo(WorkspaceRole.MEMBER);
    }

    // ───── B-WS-10: 멤버 제거 후 접근 → 403 ─────
    @Test
    void B_WS_10_멤버_제거_후_접근_거부() throws Exception {
        workspaceService.removeMember(workspace.getId(), viewer.getId(), owner.getId());

        mockMvc.perform(get("/api/v1/workspaces/" + workspace.getId())
                        .with(MockUserSupport.mockUser(viewer.getId())))
                .andExpect(status().isForbidden());
    }

    // ───── 추가: 내 워크스페이스 목록 조회 ─────
    @Test
    void 내_워크스페이스_목록_조회() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces")
                        .with(MockUserSupport.mockUser(owner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workspace.getId().toString()));
    }
}
