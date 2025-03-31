package com.moviesocial.service;

import com.moviesocial.model.Notification;
import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.ReviewDislike;
import com.moviesocial.model.ReviewLike;
import com.moviesocial.model.User;
import com.moviesocial.repository.ReviewCommentRepository;
import com.moviesocial.repository.ReviewDislikeRepository;
import com.moviesocial.repository.ReviewLikeRepository;
import com.moviesocial.repository.ReviewRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.payload.request.MovieReviewRequest;
import com.moviesocial.payload.response.MovieReviewResponse;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private ReviewLikeRepository reviewLikeRepository;
    
    @Autowired
    private ReviewDislikeRepository reviewDislikeRepository;
    
    @Autowired
    private ReviewCommentRepository reviewCommentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    // 모든 리뷰 가져오기
    @Transactional(readOnly = true)
    public Page<MovieReviewResponse> getAllReviews(Pageable pageable, Long userId) {
        Page<Review> reviews = reviewRepository.findAllWithDetails(pageable);
        return reviews.map(review -> convertToResponse(review, userId));
    }
    
    // 특정 유저의 리뷰 가져오기
    public Page<MovieReviewResponse> getUserReviews(String username, Pageable pageable, Long currentUserId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Page<Review> reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return reviews.map(review -> convertToResponse(review, currentUserId));
    }
    
    // 검색으로 리뷰 찾기
    public Page<MovieReviewResponse> searchReviews(String keyword, Pageable pageable, Long userId) {
        Page<Review> reviews = reviewRepository.searchByKeyword(keyword, pageable);
        return reviews.map(review -> convertToResponse(review, userId));
    }
    
    // 특정 영화의 리뷰 가져오기
    public Page<MovieReviewResponse> getMovieReviews(Long movieId, Pageable pageable, Long userId) {
        Page<Review> reviews = reviewRepository.findByMovieId(movieId, pageable);
        return reviews.map(review -> convertToResponse(review, userId));
    }
    
    // 리뷰 세부 정보 가져오기
    public MovieReviewResponse getReviewById(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        
        return convertToResponse(review, userId);
    }
    
    // 리뷰 생성하기
    @Transactional
    public MovieReviewResponse createReview(MovieReviewRequest reviewRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Review review = Review.builder()
                .title(reviewRequest.getTitle())
                .content(reviewRequest.getContent())
                .rating(reviewRequest.getRating())
                .movieId(reviewRequest.getMovieId())
                .movieTitle(reviewRequest.getMovieTitle())
                .moviePoster(reviewRequest.getMoviePoster())
                .isSpoiler(reviewRequest.getIsSpoiler())
                .likeCount(0)
                .dislikeCount(0)
                .commentCount(0)
                .user(user)
                .build();
        
        review = reviewRepository.save(review);
        
        return convertToResponse(review, userId);
    }
    
    // 리뷰 수정하기
    @Transactional
    public MovieReviewResponse updateReview(Long reviewId, MovieReviewRequest reviewRequest, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("리뷰를 수정할 권한이 없습니다.");
        }
        
        review.setTitle(reviewRequest.getTitle());
        review.setContent(reviewRequest.getContent());
        review.setRating(reviewRequest.getRating());
        review.setIsSpoiler(reviewRequest.getIsSpoiler());
        
        review = reviewRepository.save(review);
        
        return convertToResponse(review, userId);
    }
    
    // 리뷰 삭제하기
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("리뷰를 삭제할 권한이 없습니다.");
        }
        
        reviewRepository.delete(review);
    }
    
    // 리뷰 좋아요
    @Transactional
    public MovieReviewResponse likeReview(Long reviewId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        
        // 이미 좋아요한 경우 좋아요 취소
        if (reviewLikeRepository.existsByReviewAndUser(review, user)) {
            reviewLikeRepository.deleteByReviewAndUser(review, user);
        } else {
            // 싫어요가 있으면 제거
            if (reviewDislikeRepository.existsByReviewAndUser(review, user)) {
                reviewDislikeRepository.deleteByReviewAndUser(review, user);
            }
            
            // 좋아요 추가
            ReviewLike like = ReviewLike.builder()
                    .review(review)
                    .user(user)
                    .build();
            
            reviewLikeRepository.save(like);
        }
        
        return convertToResponse(review, userId);
    }
    
    // 리뷰 싫어요
    @Transactional
    public MovieReviewResponse dislikeReview(Long reviewId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        
        // 이미 싫어요한 경우 싫어요 취소
        if (reviewDislikeRepository.existsByReviewAndUser(review, user)) {
            reviewDislikeRepository.deleteByReviewAndUser(review, user);
        } else {
            // 좋아요가 있으면 제거
            if (reviewLikeRepository.existsByReviewAndUser(review, user)) {
                reviewLikeRepository.deleteByReviewAndUser(review, user);
            }
            
            // 싫어요 추가
            ReviewDislike dislike = ReviewDislike.builder()
                    .review(review)
                    .user(user)
                    .build();
            
            reviewDislikeRepository.save(dislike);
        }
        
        return convertToResponse(review, userId);
    }
    
    // 리뷰를 MovieReviewResponse로 변환
    private MovieReviewResponse convertToResponse(Review review, Long currentUserId) {
        boolean isLiked = false;
        boolean isDisliked = false;
        
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                isLiked = reviewLikeRepository.existsByReviewAndUser(review, currentUser);
                isDisliked = reviewDislikeRepository.existsByReviewAndUser(review, currentUser);
            }
        }
        
        User user = review.getUser();
        
        return MovieReviewResponse.builder()
                .id(review.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .userProfileUrl(user.getProfileImageUrl())
                .title(review.getTitle())
                .content(review.getContent())
                .rating(review.getRating())
                .movieId(review.getMovieId())
                .movieTitle(review.getMovieTitle())
                .moviePoster(review.getMoviePoster())
                .isSpoiler(review.getIsSpoiler())
                .likeCount((int) reviewLikeRepository.countByReview(review))
                .dislikeCount((int) reviewDislikeRepository.countByReview(review))
                .commentCount(review.getCommentCount())
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
    
    // 리뷰 댓글 추가
    @Transactional
    public ReviewComment addComment(ReviewComment comment) {
        if (comment.getUser() == null) {
            throw new IllegalArgumentException("댓글 작성자 정보가 없습니다.");
        }

        // 리뷰의 댓글 카운트 증가
        Review review = comment.getReview();
        review.setCommentCount(review.getCommentCount() + 1);
        reviewRepository.save(review);
        
        // 댓글 저장
        ReviewComment savedComment = reviewCommentRepository.save(comment);
        
        // 리뷰의 comments Set에 댓글 추가
        review.getComments().add(savedComment);
        reviewRepository.save(review);
        
        return savedComment;
    }
    
    // 리뷰 댓글 가져오기
    public List<ReviewComment> getReviewComments(Long reviewId) {
        return reviewCommentRepository.findByReviewIdWithDetails(reviewId)
            .stream()
            .peek(comment -> {
                // 초기화 보장
                Hibernate.initialize(comment.getUser());
            })
            .collect(Collectors.toList());
    }
    
    // 최근 리뷰 가져오기
    public List<MovieReviewResponse> getRecentReviews(int count, Long userId) {
        Pageable pageable = Pageable.ofSize(count);
        Page<Review> reviews = reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        return reviews.getContent().stream()
                .map(review -> convertToResponse(review, userId))
                .collect(Collectors.toList());
    }
    
    // 유저의 최근 리뷰 가져오기
    public List<MovieReviewResponse> getUserRecentReviews(String username, int count, Long currentUserId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
        
        Pageable pageable = Pageable.ofSize(count);
        Page<Review> reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return reviews.getContent().stream()
                .map(review -> convertToResponse(review, currentUserId))
                .collect(Collectors.toList());
    }

    public Optional<Review> getReviewEntityById(Long reviewId) {
        return reviewRepository.findById(reviewId);
    }
} 