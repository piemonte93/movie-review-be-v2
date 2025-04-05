package com.moviesocial.controller;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.CreateReviewRequest;
import com.moviesocial.payload.request.UpdateReviewRequest;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.repository.ReviewRepository;
import com.moviesocial.repository.UserRepository;
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
public class TvShowReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * TV 쇼에 대한 리뷰를 작성하는 API
     * @param userDetails 인증된 사용자 정보
     * @param request 리뷰 작성 요청
     * @return 작성된 리뷰
     */
    @PostMapping("/tvreview")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createTvReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateReviewRequest request) {
        return ResponseEntity.ok(reviewService.createTvReview(
                userDetails.getUsername(),
                request.getMovie_id(),
                request.getTitle(),
                request.getContent(),
                request.getRating(),
                request.getIs_spoiler()
        ));
    }
    
    /**
     * TV 쇼 리뷰를 수정하는 API
     * @param userDetails 인증된 사용자 정보
     * @param reviewId 리뷰 ID
     * @param request 리뷰 수정 요청
     * @return 수정된 리뷰
     */
    @PutMapping("/tvreviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReviewResponse> updateTvReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.updateTvReview(
                reviewId,
                userDetails.getUsername(),
                request.getTitle(),
                request.getContent(),
                request.getRating(),
                request.getIs_spoiler(),
                request.getMovie_id(),
                request.getMovie_title(),
                request.getMovie_poster_path()
        ));
    }
    
    /**
     * TV 쇼 리뷰를 삭제하는 API
     * @param userDetails 인증된 사용자 정보
     * @param reviewId 리뷰 ID
     * @return 성공 메시지
     */
    @DeleteMapping("/tvreviews/{reviewId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteTvReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        reviewService.deleteTvReview(reviewId, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }
    
    /**
     * TV 쇼의 리뷰 목록을 가져오는 API
     * @param tvId TV 쇼 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return TV 쇼 리뷰 목록
     */
    @GetMapping("/tvreviews/tv/{tvId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByTvId(
            @PathVariable Long tvId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getReviewsByTvId(tvId, page, size));
    }

    /**
     * 모든 TV 쇼 리뷰 목록을 가져오는 API
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return TV 쇼 리뷰 목록
     */
    @GetMapping("/tvreviews")
    public ResponseEntity<Map<String, Object>> getAllTvReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ReviewResponse> reviewsPage = reviewService.getAllTvReviews(page, size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", reviewsPage.getContent());
        response.put("totalElements", reviewsPage.getTotalElements());
        response.put("totalPages", reviewsPage.getTotalPages());
        response.put("currentPage", reviewsPage.getNumber());
        response.put("size", reviewsPage.getSize());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자의 TV 쇼 리뷰 목록을 가져오는 API
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자의 TV 쇼 리뷰 목록
     */
    @GetMapping("/tvreviews/user/{username}")
    public ResponseEntity<Page<ReviewResponse>> getUserTvReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getUserTvReviews(username, page, size));
    }

    /**
     * 사용자가 특정 TV 쇼에 대한 리뷰를 이미 작성했는지 확인하는 API
     * @param tvId TV 쇼 ID
     * @param userDetails 인증된 사용자 정보
     * @return 리뷰 존재 여부 (exists: true/false)
     */
    @GetMapping("/tvreviews/check")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Boolean>> checkUserReviewForTv(
            @RequestParam(name = "tv_id") Long tvId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        List<Review> existingReviews = reviewRepository.findByUserIdAndMovieIdAndContentType(user.getId(), tvId, "tv");
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", !existingReviews.isEmpty());
        
        return ResponseEntity.ok(response);
    }
} 