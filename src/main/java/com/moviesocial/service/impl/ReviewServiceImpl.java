package com.moviesocial.service.impl;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.repository.ReviewRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.ReviewService;
import com.moviesocial.service.TmdbApiService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TmdbApiService tmdbApiService;

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return reviewRepository.findByUser(user, PageRequest.of(page, size))
                .map(this::convertToReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByMovieId(Long movieId, int page, int size) {
        return reviewRepository.findByMovieId(movieId, PageRequest.of(page, size))
                .map(this::convertToReviewResponse);
    }
    
    @Override
    @Transactional
    public ReviewResponse createReview(String username, Long movieId, String content, Double rating) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 이미 해당 영화에 대한 리뷰가 있는지 확인
        List<Review> existingReviews = reviewRepository.findByUserIdAndMovieId(user.getId(), movieId);
        if (!existingReviews.isEmpty()) {
            throw new RuntimeException("이미 이 영화에 대한 리뷰를 작성하셨습니다. 기존 리뷰를 수정하시겠습니까?");
        }

        // 영화 정보 가져오기
        ContentDetail movieDetail = tmdbApiService.getMovieDetails(movieId);
        if (movieDetail == null) {
            throw new RuntimeException("영화 정보를 찾을 수 없습니다.");
        }

        Review review = Review.builder()
                .user(user)
                .movieId(movieId)
                .movieTitle(movieDetail.getTitle())
                .moviePoster(movieDetail.getPosterPath())
                .content(content)
                .rating(rating)
                .title(content.substring(0, Math.min(100, content.length())))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return convertToReviewResponse(reviewRepository.save(review));
    }
    
    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, String username, String content, Double rating) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 리뷰를 수정할 권한이 없습니다.");
        }

        review.setContent(content);
        review.setRating(rating);
        review.setTitle(content.substring(0, Math.min(100, content.length())));

        return convertToReviewResponse(reviewRepository.save(review));
    }
    
    @Override
    @Transactional
    public void deleteReview(Long reviewId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 리뷰를 삭제할 권한이 없습니다.");
        }

        reviewRepository.delete(review);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews(int page, int size) {
        return reviewRepository.findAll(PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::convertToReviewResponse)
                .toList();
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalReviews() {
        return reviewRepository.count();
    }
    
    private ReviewResponse convertToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .username(review.getUser().getUsername())
                .userProfileImageUrl(review.getUser().getProfileImageUrl())
                .movieId(review.getMovieId())
                .movieTitle(review.getMovieTitle())
                .moviePosterPath(review.getMoviePoster())
                .content(review.getContent())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
} 