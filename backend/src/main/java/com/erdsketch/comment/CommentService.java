package com.erdsketch.comment;

import com.erdsketch.common.exception.ResourceNotFoundException;
import com.erdsketch.document.DocumentRepository;
import com.erdsketch.document.ErdDocument;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final WorkspaceMemberRepository memberRepository;

    @Transactional
    public CommentResponse create(UUID documentId, CreateCommentRequest request, UUID userId) {
        ErdDocument doc = findDocAndCheck(documentId, userId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        CommentTargetType targetType;
        try {
            targetType = CommentTargetType.valueOf(request.targetType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid target type: " + request.targetType());
        }

        Comment comment = Comment.builder()
                .document(doc)
                .author(author)
                .targetType(targetType)
                .targetId(request.targetId())
                .content(request.content())
                .resolved(false)
                .parentId(request.parentId())
                .build();

        return CommentResponse.from(commentRepository.save(comment), List.of());
    }

    public List<CommentResponse> list(UUID documentId, UUID userId, Boolean resolvedFilter) {
        findDocAndCheck(documentId, userId);

        List<Comment> roots;
        if (Boolean.FALSE.equals(resolvedFilter)) {
            roots = commentRepository.findByDocumentIdAndParentIdIsNullAndResolvedFalseOrderByCreatedAtAsc(documentId);
        } else {
            roots = commentRepository.findByDocumentIdAndParentIdIsNullOrderByCreatedAtAsc(documentId);
        }

        return roots.stream()
                .map(c -> {
                    List<CommentResponse> replies = commentRepository
                            .findByParentIdOrderByCreatedAtAsc(c.getId())
                            .stream()
                            .map(r -> CommentResponse.from(r, List.of()))
                            .toList();
                    return CommentResponse.from(c, replies);
                })
                .toList();
    }

    @Transactional
    public CommentResponse update(UUID commentId, UpdateCommentRequest request, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Only the author can update this comment");
        }

        comment.setContent(request.content());
        return CommentResponse.from(commentRepository.save(comment), getReplies(comment.getId()));
    }

    @Transactional
    public void delete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Only the author can delete this comment");
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public CommentResponse resolve(UUID commentId, UUID userId, boolean resolved) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        checkAccess(comment.getDocument().getProject().getWorkspace().getId(), userId);

        comment.setResolved(resolved);
        return CommentResponse.from(commentRepository.save(comment), getReplies(comment.getId()));
    }

    private List<CommentResponse> getReplies(UUID parentId) {
        return commentRepository.findByParentIdOrderByCreatedAtAsc(parentId)
                .stream()
                .map(r -> CommentResponse.from(r, List.of()))
                .toList();
    }

    private ErdDocument findDocAndCheck(UUID documentId, UUID userId) {
        ErdDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        checkAccess(doc.getProject().getWorkspace().getId(), userId);
        return doc;
    }

    private void checkDocAccess(UUID documentId, UUID userId) {
        ErdDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        checkAccess(doc.getProject().getWorkspace().getId(), userId);
    }

    private void checkAccess(UUID workspaceId, UUID userId) {
        if (!memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
