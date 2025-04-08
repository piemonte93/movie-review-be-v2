package com.moviesocial.controller;

import com.moviesocial.model.Comment;
import com.moviesocial.payload.request.CommentRequest;
import com.moviesocial.payload.response.CommentResponse;
import com.moviesocial.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @PostMapping("/posts/{postId}")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestParam Long userId,
            @RequestBody CommentRequest commentRequest) {
        return ResponseEntity.ok(commentService.createComment(postId, commentRequest, userId));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<Page<Comment>> getComments(
            @PathVariable Long postId,
            Pageable pageable) {
        return ResponseEntity.ok(commentService.getComments(postId, pageable));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestParam Long userId) {
        commentService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }
} 