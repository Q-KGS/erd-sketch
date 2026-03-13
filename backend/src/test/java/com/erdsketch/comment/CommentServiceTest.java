package com.erdsketch.comment;

import com.erdsketch.document.DocumentRepository;
import com.erdsketch.document.ErdDocument;
import com.erdsketch.project.DbType;
import com.erdsketch.project.Project;
import com.erdsketch.user.User;
import com.erdsketch.user.UserRepository;
import com.erdsketch.workspace.Workspace;
import com.erdsketch.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock CommentRepository commentRepository;
    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock WorkspaceMemberRepository memberRepository;
    @InjectMocks CommentService commentService;

    UUID userId;
    UUID workspaceId;
    UUID documentId;
    User user;
    User anotherUser;
    ErdDocument document;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        documentId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("test@example.com")
                .displayName("Test User")
                .passwordHash("hash")
                .build();

        anotherUser = User.builder()
                .id(UUID.randomUUID())
                .email("other@example.com")
                .displayName("Other User")
                .passwordHash("hash")
                .build();

        Workspace workspace = Workspace.builder()
                .id(workspaceId)
                .name("Test Workspace")
                .slug("test-ws")
                .owner(user)
                .build();

        Project project = Project.builder()
                .id(UUID.randomUUID())
                .workspace(workspace)
                .name("Test Project")
                .targetDbType(DbType.POSTGRESQL)
                .createdBy(user)
                .build();

        document = ErdDocument.builder()
                .id(documentId)
                .project(project)
                .name("Test ERD")
                .build();
    }

    // ───── B-CMT-01: 댓글 생성 ─────
    @Test
    void B_CMT_01_댓글_생성_성공() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(commentRepository.save(any(Comment.class))).willAnswer(inv -> {
            Comment c = inv.getArgument(0);
            return Comment.builder()
                    .id(UUID.randomUUID())
                    .document(c.getDocument())
                    .author(c.getAuthor())
                    .targetType(c.getTargetType())
                    .targetId(c.getTargetId())
                    .content(c.getContent())
                    .resolved(c.isResolved())
                    .parentId(c.getParentId())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        });

        CreateCommentRequest request = new CreateCommentRequest("TABLE", "table-1", "Test comment", null);

        // when
        CommentResponse resp = commentService.create(documentId, request, userId);

        // then
        assertThat(resp.content()).isEqualTo("Test comment");
        assertThat(resp.targetType()).isEqualTo("TABLE");
        assertThat(resp.resolved()).isFalse();
    }

    // ───── B-CMT-02: 잘못된 targetType → IllegalArgumentException ─────
    @Test
    void B_CMT_02_잘못된_타깃_타입_IllegalArgumentException() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        CreateCommentRequest request = new CreateCommentRequest("INVALID_TYPE", null, "Test", null);

        // when & then
        assertThatThrownBy(() -> commentService.create(documentId, request, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid target type");
    }

    // ───── B-CMT-03: 댓글 목록 조회 ─────
    @Test
    void B_CMT_03_댓글_목록_조회() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);

        Comment comment = Comment.builder()
                .id(UUID.randomUUID())
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("A comment")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findByDocumentIdAndParentIdIsNullOrderByCreatedAtAsc(documentId)).willReturn(List.of(comment));
        given(commentRepository.findByParentIdOrderByCreatedAtAsc(comment.getId())).willReturn(List.of());

        // when
        List<CommentResponse> list = commentService.list(documentId, userId, null);

        // then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).content()).isEqualTo("A comment");
    }

    // ───── B-CMT-04: resolved=false 필터링 ─────
    @Test
    void B_CMT_04_미해결_댓글만_조회() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);

        Comment unresolvedComment = Comment.builder()
                .id(UUID.randomUUID())
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("Unresolved")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findByDocumentIdAndParentIdIsNullAndResolvedFalseOrderByCreatedAtAsc(documentId))
                .willReturn(List.of(unresolvedComment));
        given(commentRepository.findByParentIdOrderByCreatedAtAsc(unresolvedComment.getId())).willReturn(List.of());

        // when
        List<CommentResponse> list = commentService.list(documentId, userId, false);

        // then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).resolved()).isFalse();
        verify(commentRepository).findByDocumentIdAndParentIdIsNullAndResolvedFalseOrderByCreatedAtAsc(documentId);
    }

    // ───── B-CMT-05: 댓글 수정 성공 ─────
    @Test
    void B_CMT_05_댓글_수정_성공() {
        // given
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("Original content")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentRepository.findByParentIdOrderByCreatedAtAsc(commentId)).willReturn(List.of());

        // when
        CommentResponse resp = commentService.update(commentId, new UpdateCommentRequest("Updated content"), userId);

        // then
        assertThat(comment.getContent()).isEqualTo("Updated content");
    }

    // ───── B-CMT-06: 작성자가 아닌 사용자가 수정 → AccessDeniedException ─────
    @Test
    void B_CMT_06_비작성자_댓글_수정_거부() {
        // given
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .document(document)
                .author(anotherUser)
                .targetType(CommentTargetType.TABLE)
                .content("Author's comment")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.update(commentId, new UpdateCommentRequest("New content"), userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ───── B-CMT-07: 댓글 삭제 성공 ─────
    @Test
    void B_CMT_07_댓글_삭제_성공() {
        // given
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("To be deleted")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when
        commentService.delete(commentId, userId);

        // then
        verify(commentRepository).delete(comment);
    }

    // ───── B-CMT-08: 작성자가 아닌 사용자가 삭제 → AccessDeniedException ─────
    @Test
    void B_CMT_08_비작성자_댓글_삭제_거부() {
        // given
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .document(document)
                .author(anotherUser)
                .targetType(CommentTargetType.TABLE)
                .content("Author's comment")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.delete(commentId, userId))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ───── B-CMT-09: 댓글 resolve ─────
    @Test
    void B_CMT_09_댓글_해결_처리() {
        // given
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("Issue to resolve")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentRepository.findByParentIdOrderByCreatedAtAsc(commentId)).willReturn(List.of());

        // when
        CommentResponse resp = commentService.resolve(commentId, userId, true);

        // then
        assertThat(comment.isResolved()).isTrue();
    }

    // ───── B-CMT-10: 댓글 reopen ─────
    @Test
    void B_CMT_10_댓글_재오픈_처리() {
        // given
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("Resolved comment")
                .resolved(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
        given(commentRepository.findByParentIdOrderByCreatedAtAsc(commentId)).willReturn(List.of());

        // when
        commentService.resolve(commentId, userId, false);

        // then
        assertThat(comment.isResolved()).isFalse();
    }

    // ───── B-CMT-11: 답글 목록 조회 (루트 댓글의 replies 포함) ─────
    @Test
    void B_CMT_11_루트_댓글에_답글_포함() {
        // given
        given(documentRepository.findById(documentId)).willReturn(Optional.of(document));
        given(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).willReturn(true);

        UUID rootCommentId = UUID.randomUUID();
        Comment rootComment = Comment.builder()
                .id(rootCommentId)
                .document(document)
                .author(user)
                .targetType(CommentTargetType.TABLE)
                .content("Root comment")
                .resolved(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Comment reply = Comment.builder()
                .id(UUID.randomUUID())
                .document(document)
                .author(anotherUser)
                .targetType(CommentTargetType.TABLE)
                .content("Reply comment")
                .resolved(false)
                .parentId(rootCommentId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commentRepository.findByDocumentIdAndParentIdIsNullOrderByCreatedAtAsc(documentId)).willReturn(List.of(rootComment));
        given(commentRepository.findByParentIdOrderByCreatedAtAsc(rootCommentId)).willReturn(List.of(reply));

        // when
        List<CommentResponse> list = commentService.list(documentId, userId, null);

        // then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).replies()).hasSize(1);
        assertThat(list.get(0).replies().get(0).content()).isEqualTo("Reply comment");
    }
}
