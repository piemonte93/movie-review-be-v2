package com.moviesocial.service.impl;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.User;
import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.payload.response.ReviewCommentResponse;
import com.moviesocial.repository.ReviewRepository;
import com.moviesocial.repository.ReviewCommentRepository;
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
    private final ReviewCommentRepository reviewCommentRepository;
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
    public ReviewResponse createReview(String username, Long movieId, String title, String content, Double rating, Boolean isSpoiler) {
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
                .title(title)
                .content(content)
                .rating(rating)
                .isSpoiler(isSpoiler != null ? isSpoiler : false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return convertToReviewResponse(reviewRepository.save(review));
    }
    
    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, String username, String title, String content, Double rating, Boolean isSpoiler,
                                    Long movieId, String movieTitle, String moviePoster) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 리뷰를 수정할 권한이 없습니다.");
        }
        
        // 제목과 내용을 별도로 업데이트
        review.setTitle(title);
        review.setContent(content);
        review.setRating(rating);
        
        // isSpoiler가 null이 아니면 업데이트
        if (isSpoiler != null) {
            review.setIsSpoiler(isSpoiler);
        }
        
        // 영화 정보 업데이트 (값이 제공된 경우에만)
        if (movieId != null) {
            review.setMovieId(movieId);
        }
        
        if (movieTitle != null) {
            review.setMovieTitle(movieTitle);
        }
        
        if (moviePoster != null) {
            review.setMoviePoster(moviePoster);
        }
        
        // 업데이트 시간 설정
        review.setUpdatedAt(LocalDateTime.now());
        
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
    public Page<ReviewResponse> getAllReviews(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findAll(pageRequest).map(this::convertToReviewResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getTotalReviews() {
        return reviewRepository.count();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewCommentResponse> getReviewComments(Long reviewId, int page, int size) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        return reviewCommentRepository.findByReview(review, PageRequest.of(page, size))
                .map(this::convertToCommentResponse);
    }

    @Override
    @Transactional
    public ReviewCommentResponse createReviewComment(Long reviewId, String username, String content) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));

        ReviewComment comment = ReviewComment.builder()
                .review(review)
                .user(user)
                .content(content)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        review.setCommentCount(review.getCommentCount() + 1);
        reviewRepository.save(review);

        return convertToCommentResponse(reviewCommentRepository.save(comment));
    }

    @Override
    @Transactional
    public ReviewCommentResponse updateReviewComment(Long reviewId, Long commentId, String username, String content) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        if (!comment.getReview().getId().equals(reviewId)) {
            throw new RuntimeException("잘못된 리뷰의 댓글입니다.");
        }

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 댓글을 수정할 권한이 없습니다.");
        }

        comment.setContent(content);
        comment.setUpdatedAt(LocalDateTime.now());

        return convertToCommentResponse(reviewCommentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteReviewComment(Long reviewId, Long commentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        if (!comment.getReview().getId().equals(reviewId)) {
            throw new RuntimeException("잘못된 리뷰의 댓글입니다.");
        }

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 댓글을 삭제할 권한이 없습니다.");
        }

        Review review = comment.getReview();
        review.setCommentCount(review.getCommentCount() - 1);
        reviewRepository.save(review);

        reviewCommentRepository.delete(comment);
    }

    @Override
    @Transactional
    public void likeReviewComment(Long reviewId, Long commentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        if (!comment.getReview().getId().equals(reviewId)) {
            throw new RuntimeException("잘못된 리뷰의 댓글입니다.");
        }

        // TODO: 좋아요 로직 구현
    }

    @Override
    @Transactional
    public void dislikeReviewComment(Long reviewId, Long commentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));

        if (!comment.getReview().getId().equals(reviewId)) {
            throw new RuntimeException("잘못된 리뷰의 댓글입니다.");
        }

        // TODO: 싫어요 로직 구현
    }

    private ReviewResponse convertToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .userProfileImageUrl(review.getUser().getProfileImageUrl())
                .movieId(review.getMovieId())
                .title(review.getTitle())
                .movieTitle(review.getMovieTitle())
                .moviePosterPath(review.getMoviePoster())
                .content(review.getContent())
                .rating(review.getRating())
                .isSpoiler(review.getIsSpoiler())
                .likeCount(review.getLikeCount())
                .dislikeCount(review.getDislikeCount())
                .commentCount(review.getCommentCount())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }

    private ReviewCommentResponse convertToCommentResponse(ReviewComment comment) {
        return ReviewCommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
} 