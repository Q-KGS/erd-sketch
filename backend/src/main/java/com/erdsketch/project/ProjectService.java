package com.erdsketch.project;

import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import com.erdsketch.workspace.Workspace;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import com.erdsketch.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse create(UUID workspaceId, CreateProjectRequest request, UUID userId) {
        checkAccess(workspaceId, userId);
        Workspace workspace = workspaceRepository.getReferenceById(workspaceId);
        User creator = userRepository.getReferenceById(userId);
        Project project = Project.builder()
                .workspace(workspace)
                .name(request.name())
                .description(request.description())
                .targetDbType(request.targetDbType())
                .createdBy(creator)
                .build();
        projectRepository.save(project);
        return ProjectResponse.from(project);
    }

    public List<ProjectResponse> list(UUID workspaceId, UUID userId) {
        checkAccess(workspaceId, userId);
        return projectRepository.findByWorkspaceId(workspaceId)
                .stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse get(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        checkAccess(project.getWorkspace().getId(), userId);
        return ProjectResponse.from(project);
    }

    @Transactional
    public ProjectResponse update(UUID projectId, UpdateProjectRequest request, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        checkAccess(project.getWorkspace().getId(), userId);
        if (request.name() != null) project.setName(request.name());
        if (request.description() != null) project.setDescription(request.description());
        if (request.targetDbType() != null) project.setTargetDbType(request.targetDbType());
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID projectId, UUID userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
        checkAccess(project.getWorkspace().getId(), userId);
        projectRepository.delete(project);
    }

    private void checkAccess(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
