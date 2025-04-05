package com.moviesocial.service.impl;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.User;
import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.payload.response.ReviewCommentResponse;
import com.moviesocial.payload.response.UserResponse;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return reviewRepository.findByUserAndContentType(user, "movie", PageRequest.of(page, size))
                .map(this::convertToReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByMovieId(Long movieId, int page, int size) {
        return reviewRepository.findByContentTypeAndMovieId("movie", movieId, PageRequest.of(page, size))
                .map(this::convertToReviewResponse);
    }
    
    @Override
    @Transactional
    public ReviewResponse createReview(String username, Long movieId, String title, String content, Double rating, Boolean isSpoiler) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 이미 해당 영화에 대한 리뷰가 있는지 확인
        List<Review> existingReviews = reviewRepository.findByUserIdAndMovieIdAndContentType(user.getId(), movieId, "movie");
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
                .contentType("movie")
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

        if (movieId != null && !movieId.equals(review.getMovieId())) {
            // 리뷰가 속한 영화가 변경되었는지 확인
            List<Review> existingReviews = reviewRepository.findByUserIdAndMovieIdAndContentType(user.getId(), movieId, "movie");
            if (!existingReviews.isEmpty() && !existingReviews.get(0).getId().equals(reviewId)) {
                throw new RuntimeException("이미 선택한 영화에 대한 리뷰를 작성하셨습니다.");
            }
            review.setMovieId(movieId);
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
        return reviewRepository.findByContentType("movie", pageRequest).map(this::convertToReviewResponse);
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
        System.out.println("댓글 삭제 요청 시작 - reviewId: " + reviewId + ", commentId: " + commentId + ", username: " + username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        System.out.println("사용자 확인 완료 - userId: " + user.getId());

        ReviewComment comment = reviewCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다."));
        System.out.println("댓글 조회 완료 - commentId: " + comment.getId());

        if (!comment.getReview().getId().equals(reviewId)) {
            throw new RuntimeException("잘못된 리뷰의 댓글입니다.");
        }

        if (!comment.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 댓글을 삭제할 권한이 없습니다.");
        }
        System.out.println("댓글 삭제 권한 확인 완료");

        // 먼저 댓글 수 업데이트
        Review review = comment.getReview();
        int newCommentCount = Math.max(0, review.getCommentCount() - 1);
        System.out.println("리뷰 댓글 수 업데이트 - 이전: " + review.getCommentCount() + ", 이후: " + newCommentCount);
        review.setCommentCount(newCommentCount);
        
        // 리뷰 저장 및 즉시 반영
        reviewRepository.saveAndFlush(review);
        System.out.println("리뷰 댓글 수 업데이트 저장 완료");
        
        // 명시적인 SQL 쿼리로 댓글 삭제
        try {
            // 네이티브 SQL 쿼리 사용
            String deleteQuery = "DELETE FROM review_comments WHERE id = :commentId";
            int updatedRows = entityManager.createNativeQuery(deleteQuery)
                .setParameter("commentId", commentId)
                .executeUpdate();
                
            System.out.println("댓글 삭제 완료 (직접 SQL 실행) - commentId: " + commentId + ", 영향받은 행: " + updatedRows);
            
            // 영향받은 행이 없으면 오류 발생
            if (updatedRows == 0) {
                throw new RuntimeException("댓글 삭제에 실패했습니다. 이미 삭제되었거나 존재하지 않는 댓글입니다.");
            }
        } catch (Exception e) {
            System.err.println("댓글 삭제 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("댓글 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        System.out.println("댓글 삭제 프로세스 완료");
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
    
    @Override
    @Transactional
    public ReviewResponse createTvReview(String username, Long tvId, String title, String content, Double rating, Boolean isSpoiler) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 이미 해당 TV 쇼에 대한 리뷰가 있는지 확인
        List<Review> existingReviews = reviewRepository.findByUserIdAndMovieIdAndContentType(user.getId(), tvId, "tv");
        if (!existingReviews.isEmpty()) {
            throw new RuntimeException("이미 이 TV 프로그램에 대한 리뷰를 작성하셨습니다. 기존 리뷰를 수정하시겠습니까?");
        }

        // TV 쇼 정보 가져오기
        ContentDetail tvDetail = tmdbApiService.getTvDetails(tvId);
        if (tvDetail == null) {
            throw new RuntimeException("TV 프로그램 정보를 찾을 수 없습니다.");
        }
        
        Review review = Review.builder()
                .user(user)
                .movieId(tvId)
                .movieTitle(tvDetail.getName())
                .moviePoster(tvDetail.getPosterPath())
                .title(title)
                .content(content)
                .rating(rating)
                .isSpoiler(isSpoiler != null ? isSpoiler : false)
                .contentType("tv")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        return convertToReviewResponse(reviewRepository.save(review));
    }
    
    @Override
    @Transactional
    public ReviewResponse updateTvReview(Long reviewId, String username, String title, String content, Double rating, Boolean isSpoiler,
                                     Long tvId, String tvTitle, String tvPoster) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 리뷰를 수정할 권한이 없습니다.");
        }
        
        if (!review.getContentType().equals("tv")) {
            throw new RuntimeException("이 리뷰는 TV 쇼 리뷰가 아닙니다.");
        }
        
        if (tvId != null && !tvId.equals(review.getMovieId())) {
            // 리뷰가 속한 TV 쇼가 변경되었는지 확인
            List<Review> existingReviews = reviewRepository.findByUserIdAndMovieIdAndContentType(user.getId(), tvId, "tv");
            if (!existingReviews.isEmpty() && !existingReviews.get(0).getId().equals(reviewId)) {
                throw new RuntimeException("이미 선택한 TV 프로그램에 대한 리뷰를 작성하셨습니다.");
            }
            review.setMovieId(tvId);
        }
        
        if (title != null) {
            review.setTitle(title);
        }
        
        if (content != null) {
            review.setContent(content);
        }
        
        if (rating != null) {
            review.setRating(rating);
        }
        
        if (isSpoiler != null) {
            review.setIsSpoiler(isSpoiler);
        }
        
        if (tvTitle != null) {
            review.setMovieTitle(tvTitle);
        }
        
        if (tvPoster != null) {
            review.setMoviePoster(tvPoster);
        }
        
        review.setUpdatedAt(LocalDateTime.now());
        
        return convertToReviewResponse(reviewRepository.save(review));
    }
    
    @Override
    @Transactional
    public void deleteTvReview(Long reviewId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        if (!review.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("이 리뷰를 삭제할 권한이 없습니다.");
        }
        
        if (!review.getContentType().equals("tv")) {
            throw new RuntimeException("이 리뷰는 TV 쇼 리뷰가 아닙니다.");
        }
        
        reviewRepository.delete(review);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllTvReviews(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByContentType("tv", pageRequest).map(this::convertToReviewResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByTvId(Long tvId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByContentTypeAndMovieId("tv", tvId, pageRequest).map(this::convertToReviewResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getUserTvReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        return reviewRepository.findByUserAndContentType(user, "tv", PageRequest.of(page, size))
                .map(this::convertToReviewResponse);
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
                .contentType(review.getContentType())
                .build();
    }

    private ReviewCommentResponse convertToCommentResponse(ReviewComment comment) {
        // UserResponse 생성
        UserResponse userResponse = new UserResponse();
        userResponse.setUserId(comment.getUser().getId());
        userResponse.setUsername(comment.getUser().getUsername());
        userResponse.setProfileUrl(comment.getUser().getProfileImageUrl());
        
        // 댓글 응답 설정
        ReviewCommentResponse response = ReviewCommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .user(userResponse)
                .likeCount(0)  // 현재는 좋아요/싫어요 기능이 구현되지 않아 0으로 설정
                .dislikeCount(0)
                .isLiked(false)
                .isDisliked(false)
                .build();
                
        return response;
    }
} 