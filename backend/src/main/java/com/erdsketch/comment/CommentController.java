package com.erdsketch.comment;

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
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/api/v1/documents/{documentId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable UUID documentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.create(documentId, request, principal.getId()));
    }

    @GetMapping("/api/v1/documents/{documentId}/comments")
    public ResponseEntity<List<CommentResponse>> list(
            @PathVariable UUID documentId,
            @RequestParam(required = false) Boolean resolved,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.list(documentId, principal.getId(), resolved));
    }

    @PatchMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable UUID commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.update(commentId, request, principal.getId()));
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        commentService.delete(commentId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/comments/{commentId}/resolve")
    public ResponseEntity<CommentResponse> resolve(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.resolve(commentId, principal.getId(), true));
    }

    @PostMapping("/api/v1/comments/{commentId}/reopen")
    public ResponseEntity<CommentResponse> reopen(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(commentService.resolve(commentId, principal.getId(), false));
    }
}
