package com.erdsketch.workspace;

import com.erdsketch.config.EmailService;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request, UUID ownerId) {
        if (workspaceRepository.existsBySlug(request.slug())) {
            throw new IllegalArgumentException("Slug already taken");
        }
        User owner = userRepository.getReferenceById(ownerId);
        Workspace workspace = Workspace.builder()
                .name(request.name())
                .slug(request.slug())
                .owner(owner)
                .build();
        workspaceRepository.save(workspace);

        // Add owner as OWNER member
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .build();
        memberRepository.save(member);

        return WorkspaceResponse.from(workspace);
    }

    public List<WorkspaceResponse> listForUser(UUID userId) {
        return workspaceRepository.findAllByUserId(userId)
                .stream().map(WorkspaceResponse::from).toList();
    }

    public WorkspaceResponse get(UUID workspaceId, UUID userId) {
        checkMemberAccess(workspaceId, userId);
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        return WorkspaceResponse.from(ws);
    }

    @Transactional
    public WorkspaceResponse update(UUID workspaceId, UpdateWorkspaceRequest request, UUID userId) {
        checkAdminAccess(workspaceId, userId);
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        if (request.name() != null) ws.setName(request.name());
        return WorkspaceResponse.from(ws);
    }

    @Transactional
    public void delete(UUID workspaceId, UUID userId) {
        checkOwnerAccess(workspaceId, userId);
        workspaceRepository.deleteById(workspaceId);
    }

    public List<WorkspaceMemberResponse> listMembers(UUID workspaceId, UUID userId) {
        checkMemberAccess(workspaceId, userId);
        return memberRepository.findByWorkspaceId(workspaceId)
                .stream().map(WorkspaceMemberResponse::from).toList();
    }

    @Transactional
    public WorkspaceMemberResponse inviteMember(UUID workspaceId, InviteMemberRequest request, UUID inviterId) {
        checkAdminAccess(workspaceId, inviterId);
        User invitee = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.email()));
        User inviter = userRepository.getReferenceById(inviterId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(invitee)
                .role(request.role())
                .build();
        memberRepository.save(member);

        if (emailService != null) {
            try {
                emailService.sendWorkspaceInvite(
                    invitee.getEmail(),
                    invitee.getDisplayName(),
                    workspace.getName(),
                    inviter.getDisplayName()
                );
            } catch (Exception e) {
                log.warn("Failed to send invite email to {}: {}", invitee.getEmail(), e.getMessage());
            }
        }

        return WorkspaceMemberResponse.from(member);
    }

    @Transactional
    public WorkspaceMemberResponse updateMemberRole(UUID workspaceId, UUID targetUserId, WorkspaceRole newRole, UUID requesterId) {
        checkAdminAccess(workspaceId, requesterId);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        member.setRole(newRole);
        return WorkspaceMemberResponse.from(member);
    }

    @Transactional
    public void removeMember(UUID workspaceId, UUID targetUserId, UUID requesterId) {
        checkAdminAccess(workspaceId, requesterId);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        memberRepository.delete(member);
    }

    private void checkMemberAccess(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied");
        }
    }

    private void checkAdminAccess(UUID workspaceId, UUID userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Access denied"));
        if (member.getRole() == WorkspaceRole.MEMBER || member.getRole() == WorkspaceRole.VIEWER) {
            throw new org.springframework.security.access.AccessDeniedException("Admin access required");
        }
    }

    private void checkOwnerAccess(UUID workspaceId, UUID userId) {
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Access denied"));
        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new org.springframework.security.access.AccessDeniedException("Owner access required");
        }
    }
}
