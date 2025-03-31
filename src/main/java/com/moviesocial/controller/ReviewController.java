package com.moviesocial.controller;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.CommentRequest;
import com.moviesocial.payload.request.MovieReviewRequest;
import com.moviesocial.payload.response.MovieReviewResponse;
import com.moviesocial.security.jwt.JwtUtils;
import com.moviesocial.service.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.moviesocial.dto.CommentResponse;
import com.moviesocial.payload.response.MessageResponse;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import com.moviesocial.security.services.UserDetailsImpl;

@RestController
@RequestMapping("/api/movie-reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    // 모든 리뷰 가져오기
    @GetMapping
    @Transactional
    public ResponseEntity<Page<MovieReviewResponse>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal User currentUser) {
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
                ? Sort.Direction.ASC 
                : Sort.Direction.DESC;
                
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        Page<MovieReviewResponse> reviews = reviewService.getAllReviews(pageable, userId);
        return ResponseEntity.ok(reviews);
    }
    
    // 특정 유저의 리뷰 가져오기
    @GetMapping("/user/{username}")
    public ResponseEntity<Page<MovieReviewResponse>> getUserReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        Pageable pageable = PageRequest.of(page, size);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        Page<MovieReviewResponse> reviews = reviewService.getUserReviews(username, pageable, userId);
        return ResponseEntity.ok(reviews);
    }
    
    // 특정 영화의 리뷰 가져오기
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<Page<MovieReviewResponse>> getMovieReviews(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        Pageable pageable = PageRequest.of(page, size);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        Page<MovieReviewResponse> reviews = reviewService.getMovieReviews(movieId, pageable, userId);
        return ResponseEntity.ok(reviews);
    }
    
    // 리뷰 검색하기
    @GetMapping("/search")
    public ResponseEntity<Page<MovieReviewResponse>> searchReviews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        
        Pageable pageable = PageRequest.of(page, size);
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        Page<MovieReviewResponse> reviews = reviewService.searchReviews(keyword, pageable, userId);
        return ResponseEntity.ok(reviews);
    }
    
    // 리뷰 상세 정보 가져오기
    @GetMapping("/{id}")
    public ResponseEntity<MovieReviewResponse> getReviewById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        Long userId = currentUser != null ? currentUser.getId() : null;
        
        try {
            MovieReviewResponse review = reviewService.getReviewById(id, userId);
            return ResponseEntity.ok(review);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    
    // 리뷰 작성하기
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MovieReviewResponse> createReview(
            @Valid @RequestBody MovieReviewRequest reviewRequest) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();
        
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        MovieReviewResponse createdReview = reviewService.createReview(reviewRequest, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdReview);
    }
    
    // 리뷰 수정하기
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MovieReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody MovieReviewRequest reviewRequest,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            MovieReviewResponse updatedReview = reviewService.updateReview(id, reviewRequest, currentUser.getId());
            return ResponseEntity.ok(updatedReview);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
    
    // 리뷰 삭제하기
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        try {
            reviewService.deleteReview(id, currentUser.getId());
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
    
    // 리뷰 좋아요
    @PostMapping("/{id}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MovieReviewResponse> likeReview(
            @PathVariable Long id) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();
        
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        try {
            MovieReviewResponse review = reviewService.likeReview(id, currentUser.getId());
            return ResponseEntity.ok(review);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    
    // 리뷰 싫어요
    @PostMapping("/{id}/dislike")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MovieReviewResponse> dislikeReview(
            @PathVariable Long id) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userDetails.getUser();
        
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보를 찾을 수 없습니다.");
        }
        
        try {
            MovieReviewResponse review = reviewService.dislikeReview(id, currentUser.getId());
            return ResponseEntity.ok(review);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
    
    // 리뷰에 댓글 작성하기
    @PostMapping("/{reviewId}/comments")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> addReviewComment(
            @PathVariable Long reviewId,
            @Valid @RequestBody CommentRequest request) {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            User currentUser = userDetails.getUser();
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new MessageResponse("인증된 사용자 정보를 찾을 수 없습니다. 다시 로그인해주세요."));
            }

            // Review 객체를 직접 가져옵니다
            Review review = reviewService.getReviewEntityById(reviewId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));

            // 댓글 생성
            ReviewComment comment = ReviewComment.builder()
                    .user(currentUser)
                    .review(review)
                    .content(request.getContent())
                    .likeCount(0)
                    .dislikeCount(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            // 댓글 저장
            ReviewComment savedComment = reviewService.addComment(comment);
            
            // 응답 생성
            CommentResponse response = new CommentResponse(
                    savedComment.getId(),
                    savedComment.getContent(),
                    savedComment.getUser().getUsername(),
                    savedComment.getUser().getProfileImageUrl(),
                    savedComment.getCreatedAt(),
                    savedComment.getLikeCount(),
                    savedComment.getDislikeCount(),
                    savedComment.getUser().getId()
            );

            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(new MessageResponse(e.getReason()));
        } catch (Exception e) {
            e.printStackTrace(); // 스택 트레이스 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("댓글 작성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
    
    // 최근 리뷰 가져오기
    @GetMapping("/recent")
    public ResponseEntity<List<MovieReviewResponse>> getRecentReviews(
            @RequestParam(defaultValue = "5") int count,
            @AuthenticationPrincipal User currentUser) {
        
        Long userId = currentUser != null ? currentUser.getId() : null;
        List<MovieReviewResponse> reviews = reviewService.getRecentReviews(count, userId);
        return ResponseEntity.ok(reviews);
    }
    
    // 유저의 최근 리뷰 가져오기
    @GetMapping("/user/{username}/recent")
    public ResponseEntity<List<MovieReviewResponse>> getUserRecentReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "5") int count,
            @AuthenticationPrincipal User currentUser) {
        
        Long userId = currentUser != null ? currentUser.getId() : null;
        List<MovieReviewResponse> reviews = reviewService.getUserRecentReviews(username, count, userId);
        return ResponseEntity.ok(reviews);
    }
    
    // 리뷰의 모든 댓글 가져오기
    @GetMapping("/{reviewId}/comments")
    public ResponseEntity<Map<String, Object>> getReviewComments(
            @PathVariable Long reviewId) {
        
        List<ReviewComment> comments = reviewService.getReviewComments(reviewId);
        
        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> new CommentResponse(
                        comment.getId(),
                        comment.getContent(),
                        comment.getUser().getUsername(),
                        comment.getUser().getProfileImageUrl(),
                        comment.getCreatedAt(),
                        comment.getLikeCount(),
                        comment.getDislikeCount(),
                        comment.getUser().getId()
                )).collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("comments", commentResponses);
        response.put("count", comments.size());
        
        return ResponseEntity.ok(response);
    }
} 