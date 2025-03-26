package com.moviesocial.service;

import com.moviesocial.payload.response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    /**
     * 사용자의 리뷰 목록을 가져옵니다.
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자 리뷰 목록
     */
    List<ReviewResponse> getUserReviews(String username, int page, int size);
    
    /**
     * 영화에 대한 리뷰를 작성합니다.
     * @param username 사용자명
     * @param movieId 영화 ID
     * @param content 리뷰 내용
     * @param rating 평점
     * @return 생성된 리뷰
     */
    ReviewResponse createReview(String username, Long movieId, String content, Integer rating);
    
    /**
     * 리뷰를 수정합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     * @param content 리뷰 내용
     * @param rating 평점
     * @return 수정된 리뷰
     */
    ReviewResponse updateReview(Long reviewId, String username, String content, Integer rating);
    
    /**
     * 리뷰를 삭제합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     */
    void deleteReview(Long reviewId, String username);
} 