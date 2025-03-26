package com.moviesocial.controller;

import com.moviesocial.payload.request.CreateReviewRequest;
import com.moviesocial.payload.request.UpdateReviewRequest;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.security.jwt.JwtUtils;
import com.moviesocial.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 영화에 대한 리뷰를 작성하는 API
     * @param authHeader 인증 헤더
     * @param request 리뷰 작성 요청
     * @return 작성된 리뷰
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> createReview(@RequestHeader("Authorization") String authHeader,
                                                     @RequestBody CreateReviewRequest request) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        
        ReviewResponse review = reviewService.createReview(
                username,
                request.getMovieId(),
                request.getContent(),
                request.getRating()
        );
        
        return ResponseEntity.ok(review);
    }
    
    /**
     * 리뷰를 수정하는 API
     * @param authHeader 인증 헤더
     * @param reviewId 리뷰 ID
     * @param request 리뷰 수정 요청
     * @return 수정된 리뷰
     */
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> updateReview(@RequestHeader("Authorization") String authHeader,
                                                      @PathVariable Long reviewId,
                                                      @RequestBody UpdateReviewRequest request) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        
        ReviewResponse review = reviewService.updateReview(
                reviewId,
                username,
                request.getContent(),
                request.getRating()
        );
        
        return ResponseEntity.ok(review);
    }
    
    /**
     * 리뷰를 삭제하는 API
     * @param authHeader 인증 헤더
     * @param reviewId 리뷰 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteReview(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Long reviewId) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        
        reviewService.deleteReview(reviewId, username);
        return ResponseEntity.ok(new MessageResponse("리뷰가 성공적으로 삭제되었습니다."));
    }
    
    /**
     * 영화의 리뷰 목록을 가져오는 API
     * @param movieId 영화 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 영화 리뷰 목록
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ReviewResponse>> getMovieReviews(@PathVariable Long movieId,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        // 영화 리뷰 목록 가져오기 기능 추가 필요
        // 현재 구현된 API에서는 영화별 리뷰를 가져오는 메서드가 없음
        return ResponseEntity.ok(List.of());
    }
} 