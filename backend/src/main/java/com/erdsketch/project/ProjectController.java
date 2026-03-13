package com.erdsketch.project;

import com.erdsketch.auth.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<ProjectResponse> create(@PathVariable UUID workspaceId,
                                                   @Valid @RequestBody CreateProjectRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projectService.create(workspaceId, request, principal.getId()));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects")
    public ResponseEntity<List<ProjectResponse>> list(@PathVariable UUID workspaceId,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projectService.list(workspaceId, principal.getId()));
    }

    @GetMapping("/api/v1/projects/{projectId}")
    public ResponseEntity<ProjectResponse> get(@PathVariable UUID projectId,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projectService.get(projectId, principal.getId()));
    }

    @PatchMapping("/api/v1/projects/{projectId}")
    public ResponseEntity<ProjectResponse> update(@PathVariable UUID projectId,
                                                   @RequestBody UpdateProjectRequest request,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(projectService.update(projectId, request, principal.getId()));
    }

    @DeleteMapping("/api/v1/projects/{projectId}")
    public ResponseEntity<Void> delete(@PathVariable UUID projectId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        projectService.delete(projectId, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
