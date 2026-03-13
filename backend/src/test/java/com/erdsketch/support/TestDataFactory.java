package com.erdsketch.support;

import com.erdsketch.document.ErdDocument;
import com.erdsketch.project.DbType;
import com.erdsketch.project.Project;
import com.erdsketch.user.User;
import com.erdsketch.workspace.Workspace;
import com.erdsketch.workspace.WorkspaceMember;
import com.erdsketch.workspace.WorkspaceRole;

import java.time.Instant;
import java.util.UUID;

public class TestDataFactory {

    public static User user(String emailPrefix) {
        return User.builder()
                .email(emailPrefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com")
                .displayName("Test User " + emailPrefix)
                .passwordHash("$2a$10$hashedpassword")
                .build();
    }

    public static User user() {
        return user("user");
    }

    public static Workspace workspace(User owner) {
        return Workspace.builder()
                .name("Test Workspace")
                .slug("test-ws-" + UUID.randomUUID().toString().substring(0, 8))
                .owner(owner)
                .build();
    }

    public static WorkspaceMember workspaceMember(Workspace workspace, User user, WorkspaceRole role) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(role)
                .joinedAt(Instant.now())
                .build();
    }

    public static Project project(Workspace workspace, User creator) {
        return Project.builder()
                .workspace(workspace)
                .name("Test Project")
                .targetDbType(DbType.POSTGRESQL)
                .createdBy(creator)
                .build();
    }

    public static ErdDocument erdDocument(Project project) {
        return ErdDocument.builder()
                .project(project)
                .name("Test ERD")
                .build();
    }
}
