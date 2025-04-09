package com.moviesocial.service;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.CreateReviewRequest;
import com.moviesocial.payload.request.UpdateReviewRequest;
import com.moviesocial.payload.request.CreateReviewCommentRequest;
import com.moviesocial.payload.request.UpdateReviewCommentRequest;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.payload.response.ReviewCommentResponse;
import com.moviesocial.repository.ReviewRepository;
import com.moviesocial.repository.ReviewCommentRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.repository.ReviewCommentLikeRepository;
import com.moviesocial.repository.ReviewCommentDislikeRepository;
import com.moviesocial.exception.ResourceNotFoundException;
import com.moviesocial.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface ReviewService {
    /**
     * 사용자의 리뷰 목록을 가져옵니다.
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자 리뷰 목록
     */
    Page<ReviewResponse> getUserReviews(String username, int page, int size);
    
    /**
     * 사용자의 리뷰 목록을 콘텐츠 타입에 따라 가져옵니다.
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param contentType 콘텐츠 타입 (movie 또는 tv)
     * @return 사용자 리뷰 목록
     */
    Page<ReviewResponse> getUserReviews(String username, int page, int size, String contentType);
    
    /**
     * 영화에 대한 리뷰를 작성합니다.
     * @param username 사용자명
     * @param movieId 영화 ID
     * @param title 리뷰 제목
     * @param content 리뷰 내용
     * @param rating 평점
     * @param isSpoiler 스포일러 여부
     * @return 생성된 리뷰
     */
    ReviewResponse createReview(String username, Long movieId, String title, String content, Double rating, Boolean isSpoiler);
    
    /**
     * 리뷰를 수정합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     * @param title 수정할 제목
     * @param content 수정할 내용
     * @param rating 수정할 평점
     * @param isSpoiler 스포일러 여부
     * @param movieId 영화 ID (선택적)
     * @param movieTitle 영화 제목 (선택적)
     * @param moviePoster 영화 포스터 경로 (선택적)
     * @return 수정된 리뷰
     */
    ReviewResponse updateReview(Long reviewId, String username, String title, String content, Double rating, Boolean isSpoiler,
                               Long movieId, String movieTitle, String moviePoster);
    
    /**
     * 리뷰를 삭제합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     */
    void deleteReview(Long reviewId, String username);
    
    /**
     * 모든 리뷰 목록을 가져옵니다.
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 리뷰 목록
     */
    Page<ReviewResponse> getAllReviews(int page, int size);
    
    /**
     * 전체 리뷰 수를 가져옵니다.
     * @return 전체 리뷰 수
     */
    long getTotalReviews();
    
    /**
     * 특정 영화의 리뷰 목록을 가져옵니다.
     * @param movieId 영화 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 영화 리뷰 목록
     */
    Page<ReviewResponse> getReviewsByMovieId(Long movieId, int page, int size);
    
    /**
     * 리뷰의 댓글 목록을 가져옵니다.
     * @param reviewId 리뷰 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 댓글 목록
     */
    Page<ReviewCommentResponse> getReviewComments(Long reviewId, int page, int size);
    
    /**
     * 리뷰에 댓글을 작성합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     * @param content 댓글 내용
     * @return 생성된 댓글
     */
    ReviewCommentResponse createReviewComment(Long reviewId, String username, String content);
    
    /**
     * 리뷰 댓글을 수정합니다.
     * @param reviewId 리뷰 ID
     * @param commentId 댓글 ID
     * @param username 사용자명
     * @param content 수정할 내용
     * @return 수정된 댓글
     */
    ReviewCommentResponse updateReviewComment(Long reviewId, Long commentId, String username, String content);
    
    /**
     * 리뷰 댓글을 삭제합니다.
     * @param reviewId 리뷰 ID
     * @param commentId 댓글 ID
     * @param username 사용자명
     */
    void deleteReviewComment(Long reviewId, Long commentId, String username);
    
    /**
     * 리뷰 댓글을 좋아요합니다.
     * @param reviewId 리뷰 ID
     * @param commentId 댓글 ID
     * @param username 사용자명
     */
    void likeReviewComment(Long reviewId, Long commentId, String username);
    
    /**
     * 리뷰 댓글을 싫어요합니다.
     * @param reviewId 리뷰 ID
     * @param commentId 댓글 ID
     * @param username 사용자명
     */
    void dislikeReviewComment(Long reviewId, Long commentId, String username);
    
    /**
     * TV 쇼에 대한 리뷰를 작성합니다.
     * @param username 사용자명
     * @param tvId TV 쇼 ID
     * @param title 리뷰 제목
     * @param content 리뷰 내용
     * @param rating 평점
     * @param isSpoiler 스포일러 여부
     * @return 생성된 리뷰
     */
    ReviewResponse createTvReview(String username, Long tvId, String title, String content, Double rating, Boolean isSpoiler);
    
    /**
     * TV 쇼 리뷰를 수정합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     * @param title 수정할 제목
     * @param content 수정할 내용
     * @param rating 수정할 평점
     * @param isSpoiler 스포일러 여부
     * @param tvId TV 쇼 ID (선택적)
     * @param tvTitle TV 쇼 제목 (선택적)
     * @param tvPoster TV 쇼 포스터 경로 (선택적)
     * @return 수정된 리뷰
     */
    ReviewResponse updateTvReview(Long reviewId, String username, String title, String content, Double rating, Boolean isSpoiler,
                               Long tvId, String tvTitle, String tvPoster);
    
    /**
     * TV 쇼 리뷰를 삭제합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     */
    void deleteTvReview(Long reviewId, String username);
    
    /**
     * 모든 TV 쇼 리뷰 목록을 가져옵니다.
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return TV 쇼 리뷰 목록
     */
    Page<ReviewResponse> getAllTvReviews(int page, int size);
    
    /**
     * 특정 TV 쇼의 리뷰 목록을 가져옵니다.
     * @param tvId TV 쇼 ID
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return TV 쇼 리뷰 목록
     */
    Page<ReviewResponse> getReviewsByTvId(Long tvId, int page, int size);
    
    /**
     * 사용자의 TV 쇼 리뷰 목록을 가져옵니다.
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자의 TV 쇼 리뷰 목록
     */
    Page<ReviewResponse> getUserTvReviews(String username, int page, int size);
    
    /**
     * 리뷰에 좋아요를 추가합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     * @return 업데이트된 리뷰 정보
     */
    ReviewResponse likeReview(Long reviewId, String username);
    
    /**
     * 리뷰에 싫어요를 추가합니다.
     * @param reviewId 리뷰 ID
     * @param username 사용자명
     * @return 업데이트된 리뷰 정보
     */
    ReviewResponse dislikeReview(Long reviewId, String username);
    
    /**
     * 현재 로그인한 사용자가 좋아요 누른 리뷰 목록을 가져옵니다.
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 좋아요 누른 리뷰 목록
     */
    Page<ReviewResponse> getMyLikedReviews(String username, int page, int size);
} 