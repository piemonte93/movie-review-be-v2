package com.moviesocial.controller;

import com.moviesocial.model.Review;
import com.moviesocial.payload.request.CreateReviewRequest;
import com.moviesocial.payload.request.UpdateReviewRequest;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.security.jwt.JwtUtils;
import com.moviesocial.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 영화에 대한 리뷰를 작성하는 API
     * @param userDetails 인증된 사용자 정보
     * @param request 리뷰 작성 요청
     * @return 작성된 리뷰
     */
    @PostMapping("/review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(
                userDetails.getUsername(),
                request.getMovie_id(),
                request.getContent(),
                request.getRating()
        ));
    }
    
    /**
     * 리뷰를 수정하는 API
     * @param authHeader 인증 헤더
     * @param reviewId 리뷰 ID
     * @param request 리뷰 수정 요청
     * @return 수정된 리뷰
     */
    @PutMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateReview(
                reviewId,
                userDetails.getUsername(),
                request.getContent(),
                request.getRating()
        ));
    }
    
    /**
     * 리뷰를 삭제하는 API
     * @param authHeader 인증 헤더
     * @param reviewId 리뷰 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
    
    /**
     * 영화의 리뷰 목록을 가져오는 API
     * @param movieId 영화 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 영화 리뷰 목록
     */
    @GetMapping("/reviews/movie/{movieId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByMovieId(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviewsByMovieId(movieId, page, size));
    }

    /**
     * 모든 리뷰 목록을 가져오는 API
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 리뷰 목록
     */
    @GetMapping("/reviews")
    public ResponseEntity<Map<String, Object>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<ReviewResponse> reviews = reviewService.getAllReviews(page, size);
        long totalReviews = reviewService.getTotalReviews();
        int totalPages = (int) Math.ceil((double) totalReviews / size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", reviews);
        response.put("totalElements", totalReviews);
        response.put("totalPages", totalPages);
        response.put("currentPage", page);
        response.put("size", size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/user/{username}")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getUserReviews(username, page, size));
    }
} 