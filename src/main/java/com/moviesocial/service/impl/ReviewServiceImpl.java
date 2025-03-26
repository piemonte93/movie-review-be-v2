package com.moviesocial.service.impl;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.repository.ReviewRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.ReviewService;
import com.moviesocial.service.TmdbApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TmdbApiService tmdbApiService;
    
    @Override
    public List<ReviewResponse> getUserReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Review> reviews = reviewRepository.findByUser(user, pageable);
        
        return reviews.stream()
                .map(this::convertToReviewResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public ReviewResponse createReview(String username, Long movieId, String content, Integer rating) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        // 동일한 영화에 대한 리뷰가 이미 있는지 확인
        reviewRepository.findByUserAndMovieId(user, movieId).ifPresent(review -> {
            throw new RuntimeException("이미 이 영화에 대한 리뷰를 작성했습니다.");
        });
        
        Review review = Review.builder()
                .user(user)
                .movieId(movieId)
                .content(content)
                .rating(rating)
                .createdAt(LocalDateTime.now())
                .build();
        
        Review savedReview = reviewRepository.save(review);
        return convertToReviewResponse(savedReview);
    }
    
    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, String username, String content, Integer rating) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));
        
        // 리뷰 작성자만 수정 가능
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }
        
        review.setContent(content);
        review.setRating(rating);
        review.setUpdatedAt(LocalDateTime.now());
        
        Review updatedReview = reviewRepository.save(review);
        return convertToReviewResponse(updatedReview);
    }
    
    @Override
    @Transactional
    public void deleteReview(Long reviewId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));
        
        // 리뷰 작성자만 삭제 가능
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }
        
        reviewRepository.delete(review);
    }
    
    /**
     * Review 엔티티를 ReviewResponse DTO로 변환합니다.
     * @param review Review 엔티티
     * @return ReviewResponse DTO
     */
    private ReviewResponse convertToReviewResponse(Review review) {
        // TMDB API를 통해 영화 정보 가져오기
        ContentDetail movieDetail = null;
        try {
            movieDetail = tmdbApiService.getMovieDetails(review.getMovieId());
        } catch (Exception e) {
            log.error("TMDB API 호출 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        String movieTitle = movieDetail != null ? movieDetail.getTitle() : "알 수 없는 영화";
        String posterPath = movieDetail != null ? movieDetail.getPosterPath() : null;
        
        return ReviewResponse.builder()
                .id(review.getId())
                .username(review.getUser().getUsername())
                .userProfileImageUrl(review.getUser().getProfileImageUrl())
                .movieId(review.getMovieId())
                .movieTitle(movieTitle)
                .moviePosterPath(posterPath)
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
} 