package com.moviesocial.controller;

import com.moviesocial.payload.response.PostResponse;
import com.moviesocial.payload.response.ReviewResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.moviesocial.service.PostService;
import com.moviesocial.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자가 좋아요 누른 컨텐츠(게시글, 리뷰) 관련 API를 제공하는 컨트롤러
 */
@RestController
@RequestMapping("/api/my/likes")
@RequiredArgsConstructor
public class UserLikeController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserLikeController.class);
    
    private final PostService postService;
    private final ReviewService reviewService;
    
    /**
     * 현재 로그인한 사용자가 좋아요 누른 게시글 목록을 가져옵니다.
     * @param userDetails 현재 로그인한 사용자
     * @param page 페이지 번호 (기본값 0)
     * @param size 페이지 크기 (기본값 10)
     * @return 좋아요 누른 게시글 목록
     */
    @GetMapping("/posts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<PostResponse>> getMyLikedPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        logger.info("사용자 {}의 좋아요 누른 게시글 목록 요청: page={}, size={}", userDetails.getUsername(), page, size);
        
        // UserDetails에서 사용자 ID를 직접 가져올 수 없으므로, username을 통해 처리해야 함
        // 따라서 postService 메서드도 username을 받도록 수정하거나, 내부에서 username으로 ID를 조회하는 로직 추가 필요
        
        // 임시 해결책: UserDetailsImpl를 사용하여 ID를 가져오는 방법
        long userId = 0L; // 기본값
        if (userDetails instanceof com.moviesocial.security.services.UserDetailsImpl) {
            userId = ((com.moviesocial.security.services.UserDetailsImpl) userDetails).getId();
        }
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<PostResponse> likedPosts = postService.getMyLikedPosts(userId, pageRequest);
        logger.info("좋아요 누른 게시글 {}개 반환", likedPosts.getContent().size());
        return ResponseEntity.ok(likedPosts);
    }
    
    /**
     * 현재 로그인한 사용자가 좋아요 누른 리뷰 목록을 가져옵니다.
     * @param userDetails 현재 로그인한 사용자
     * @param page 페이지 번호 (기본값 0)
     * @param size 페이지 크기 (기본값 10)
     * @return 좋아요 누른 리뷰 목록
     */
    @GetMapping("/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewResponse>> getMyLikedReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        logger.info("사용자 {}의 좋아요 누른 리뷰 목록 요청: page={}, size={}", userDetails.getUsername(), page, size);
        Page<ReviewResponse> likedReviews = reviewService.getMyLikedReviews(userDetails.getUsername(), page, size);
        logger.info("좋아요 누른 리뷰 {}개 반환", likedReviews.getContent().size());
        return ResponseEntity.ok(likedReviews);
    }
} 