package com.erdsketch.comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByDocumentIdAndParentIdIsNullOrderByCreatedAtAsc(UUID documentId);
    List<Comment> findByDocumentIdAndParentIdIsNullAndResolvedFalseOrderByCreatedAtAsc(UUID documentId);
    List<Comment> findByParentIdOrderByCreatedAtAsc(UUID parentId);
    void deleteByDocumentId(UUID documentId);
}
