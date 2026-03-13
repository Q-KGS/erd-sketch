package com.erdsketch.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsBySlug(String slug);
    Optional<Workspace> findBySlug(String slug);

    @Query("SELECT wm.workspace FROM WorkspaceMember wm WHERE wm.user.id = :userId")
    List<Workspace> findAllByUserId(UUID userId);
}
