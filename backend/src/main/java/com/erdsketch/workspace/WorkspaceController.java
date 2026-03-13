package com.erdsketch.workspace;

import com.erdsketch.auth.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@Valid @RequestBody CreateWorkspaceRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.create(request, principal.getId()));
    }

    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.listForUser(principal.getId()));
    }

    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> get(@PathVariable UUID workspaceId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.get(workspaceId, principal.getId()));
    }

    @PatchMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse> update(@PathVariable UUID workspaceId,
                                                     @RequestBody UpdateWorkspaceRequest request,
                                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.update(workspaceId, request, principal.getId()));
    }

    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID workspaceId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        workspaceService.delete(workspaceId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceId}/members")
    public ResponseEntity<List<WorkspaceMemberResponse>> listMembers(@PathVariable UUID workspaceId,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.listMembers(workspaceId, principal.getId()));
    }

    @PostMapping("/{workspaceId}/members/invite")
    public ResponseEntity<WorkspaceMemberResponse> inviteMember(@PathVariable UUID workspaceId,
                                                                  @RequestBody InviteMemberRequest request,
                                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.inviteMember(workspaceId, request, principal.getId()));
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(@PathVariable UUID workspaceId,
                                                                      @PathVariable UUID userId,
                                                                      @RequestBody UpdateMemberRoleRequest request,
                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(workspaceService.updateMemberRole(workspaceId, userId, request.role(), principal.getId()));
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID workspaceId,
                                              @PathVariable UUID userId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        workspaceService.removeMember(workspaceId, userId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
