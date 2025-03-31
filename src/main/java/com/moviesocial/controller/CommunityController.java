package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.request.CommentRequest;
import com.moviesocial.payload.request.PostRequest;
import com.moviesocial.payload.response.CommentResponse;
import com.moviesocial.payload.response.PostResponse;
import com.moviesocial.security.services.UserDetailsImpl;
import com.moviesocial.service.CommentService;
import com.moviesocial.service.PostService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private PostService postService;
    
    @Autowired
    private CommentService commentService;
    
    // 게시물 목록 조회
    @GetMapping("/posts")
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = null;
        
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userDetails.getUser();
            if (currentUser != null) {
                currentUserId = currentUser.getId();
            }
        } catch (Exception e) {
            // 인증되지 않은 사용자의 경우 currentUserId는 null로 유지
        }
        
        Page<PostResponse> posts = postService.getAllPosts(pageable, currentUserId);
        return ResponseEntity.ok(posts);
    }
    
    // 게시물 상세 조회
    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponse> getPostById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        PostResponse post = postService.getPostById(id, currentUserId);
        return ResponseEntity.ok(post);
    }
    
    // 게시물 생성
    @PostMapping("/posts")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> createPost(
            @Valid @RequestBody PostRequest postRequest) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();
        
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        PostResponse post = postService.createPost(postRequest, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(post);
    }
    
    // 게시물 수정
    @PutMapping("/posts/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequest postRequest,
            @AuthenticationPrincipal User currentUser) {
        
        PostResponse post = postService.updatePost(id, postRequest, currentUser.getId());
        return ResponseEntity.ok(post);
    }
    
    // 게시물 삭제
    @DeleteMapping("/posts/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        postService.deletePost(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }
    
    // 게시물 좋아요
    @PostMapping("/posts/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> likePost(
            @PathVariable Long id) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();
        
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        PostResponse post = postService.likePost(id, currentUser.getId());
        return ResponseEntity.ok(post);
    }
    
    // 게시물 싫어요
    @PostMapping("/posts/{id}/dislike")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PostResponse> dislikePost(
            @PathVariable Long id) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();
        
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        PostResponse post = postService.dislikePost(id, currentUser.getId());
        return ResponseEntity.ok(post);
    }
    
    // 게시물 검색
    @GetMapping("/posts/search")
    public ResponseEntity<Page<PostResponse>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "title") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        Pageable pageable = PageRequest.of(page, size);
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        
        Page<PostResponse> posts = postService.searchPosts(query, category, pageable, currentUserId);
        return ResponseEntity.ok(posts);
    }
    
    // 댓글 목록 조회
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getCommentsByPostId(
            @PathVariable Long postId,
            @AuthenticationPrincipal User currentUser) {
        
        Long currentUserId = currentUser != null ? currentUser.getId() : null;
        List<CommentResponse> comments = commentService.getCommentsByPostId(postId, currentUserId);
        return ResponseEntity.ok(comments);
    }
    
    // 댓글 생성
    @PostMapping("/posts/{postId}/comments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal User currentUser) {
        
        CommentResponse comment = commentService.createComment(postId, commentRequest, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }
    
    // 댓글 수정
    @PutMapping("/comments/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal User currentUser) {
        
        CommentResponse comment = commentService.updateComment(id, commentRequest, currentUser.getId());
        return ResponseEntity.ok(comment);
    }
    
    // 댓글 삭제
    @DeleteMapping("/comments/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        commentService.deleteComment(id, currentUser.getId());
        return ResponseEntity.ok().build();
    }
    
    // 댓글 좋아요
    @PostMapping("/comments/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> likeComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        CommentResponse comment = commentService.likeComment(id, currentUser.getId());
        return ResponseEntity.ok(comment);
    }
    
    // 댓글 싫어요
    @PostMapping("/comments/{id}/dislike")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> dislikeComment(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        CommentResponse comment = commentService.dislikeComment(id, currentUser.getId());
        return ResponseEntity.ok(comment);
    }
} 