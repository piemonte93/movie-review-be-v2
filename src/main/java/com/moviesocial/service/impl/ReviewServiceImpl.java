package com.moviesocial.service.impl;

import com.moviesocial.model.*;
import com.moviesocial.model.ERole;
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
import com.moviesocial.repository.ReviewLikeRepository;
import com.moviesocial.service.ReviewService;
import com.moviesocial.service.TmdbApiService;
import com.moviesocial.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final ReviewLikeRepository reviewLikeRepository;
    
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
    public Page<ReviewResponse> getUserReviews(String username, int page, int size, String contentType) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        if (contentType == null || contentType.isEmpty()) {
            contentType = "movie";
        }
        
        return reviewRepository.findByUserAndContentType(user, contentType, PageRequest.of(page, size))
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
        
        // 사용자가 자신의 리뷰를 삭제하거나, 관리자 또는 모더레이터인 경우 삭제 가능
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN || role.getName() == ERole.ROLE_MODERATOR);
        
        if (!review.getUser().getId().equals(user.getId()) && !isAdmin) {
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
        
        return reviewCommentRepository.findReviewCommentsByReview(review, PageRequest.of(page, size));
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

        // 사용자가 자신의 댓글을 삭제하거나, 관리자 또는 모더레이터인 경우 삭제 가능
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN || role.getName() == ERole.ROLE_MODERATOR);
        
        if (!comment.getUser().getId().equals(user.getId()) && !isAdmin) {
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
        
        // "tv" 타입인지 확인
        if (!"tv".equals(review.getContentType())) {
            throw new RuntimeException("TV 리뷰가 아닙니다.");
        }
        
        // 사용자가 자신의 리뷰를 삭제하거나, 관리자 또는 모더레이터인 경우 삭제 가능
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN || role.getName() == ERole.ROLE_MODERATOR);
        
        if (!review.getUser().getId().equals(user.getId()) && !isAdmin) {
            throw new RuntimeException("이 리뷰를 삭제할 권한이 없습니다.");
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

    @Override
    @Transactional
    public ReviewResponse likeReview(Long reviewId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        // 이미 좋아요를 눌렀는지 확인
        boolean hasLiked = review.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(user.getId()));
        
        // 이미 싫어요를 눌렀는지 확인
        boolean hasDisliked = review.getDislikes().stream()
                .anyMatch(dislike -> dislike.getUser().getId().equals(user.getId()));
        
        // 먼저 싫어요가 있으면 제거
        if (hasDisliked) {
            review.getDislikes().removeIf(dislike -> dislike.getUser().getId().equals(user.getId()));
        }
        
        // 좋아요 상태 토글
        if (hasLiked) {
            // 이미 좋아요를 눌렀다면 좋아요 취소
            review.getLikes().removeIf(like -> like.getUser().getId().equals(user.getId()));
        } else {
            // 좋아요를 누르지 않았다면 좋아요 추가
            ReviewLike reviewLike = ReviewLike.builder()
                    .review(review)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            review.getLikes().add(reviewLike);
        }
        
        review = reviewRepository.save(review);
        return convertToReviewResponse(review);
    }
    
    @Override
    @Transactional
    public ReviewResponse dislikeReview(Long reviewId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다."));
        
        // 이미 싫어요를 눌렀는지 확인
        boolean hasDisliked = review.getDislikes().stream()
                .anyMatch(dislike -> dislike.getUser().getId().equals(user.getId()));
        
        // 이미 좋아요를 눌렀는지 확인
        boolean hasLiked = review.getLikes().stream()
                .anyMatch(like -> like.getUser().getId().equals(user.getId()));
        
        // 먼저 좋아요가 있으면 제거
        if (hasLiked) {
            review.getLikes().removeIf(like -> like.getUser().getId().equals(user.getId()));
        }
        
        // 싫어요 상태 토글
        if (hasDisliked) {
            // 이미 싫어요를 눌렀다면 싫어요 취소
            review.getDislikes().removeIf(dislike -> dislike.getUser().getId().equals(user.getId()));
        } else {
            // 싫어요를 누르지 않았다면 싫어요 추가
            ReviewDislike reviewDislike = ReviewDislike.builder()
                    .review(review)
                    .user(user)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            review.getDislikes().add(reviewDislike);
        }
        
        review = reviewRepository.save(review);
        return convertToReviewResponse(review);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getMyLikedReviews(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Review> likedReviews = reviewLikeRepository.findReviewsByUser(user, pageRequest);
        
        return likedReviews.map(review -> {
            ReviewResponse response = convertToReviewResponse(review);
            
            // 현재 사용자는 이미 이 리뷰에 좋아요를 누른 상태
            response.setLiked(true);
            response.setDisliked(false);
            
            return response;
        });
    }

    @Override
    public Page<ReviewResponse> searchReviews(String query, String contentType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Review> reviews;

        if (contentType != null && !contentType.isEmpty()) {
            reviews = reviewRepository.searchByTitleOrContentAndContentType(query, contentType, pageable);
        } else {
            reviews = reviewRepository.searchByTitleOrContent(query, pageable);
        }
        
        return reviews.map(this::convertToReviewResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getHotReviews(int limit) {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        // Pageable 객체를 사용하여 가져올 리뷰 수를 제한
        Pageable pageable = PageRequest.of(0, limit); // 페이지 0, 사이즈 limit
        
        List<Review> hotReviews = reviewRepository.findHotReviews(oneMonthAgo, pageable);
        
        return hotReviews.stream()
                .map(this::convertToReviewResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double getMovieAverageRating(Long movieId) {
        List<Review> reviews = reviewRepository.findByMovieIdAndContentType(movieId, "movie");
        if (reviews.isEmpty()) {
            return null;
        }
        
        double sum = 0;
        for (Review review : reviews) {
            sum += review.getRating();
        }
        
        return sum / reviews.size();
    }
    
    @Override
    public Double getTvShowAverageRating(Long tvId) {
        List<Review> reviews = reviewRepository.findByMovieIdAndContentType(tvId, "tv");
        if (reviews.isEmpty()) {
            return null;
        }
        
        double sum = 0;
        for (Review review : reviews) {
            sum += review.getRating();
        }
        
        return sum / reviews.size();
    }
    
    private ReviewResponse convertToReviewResponse(Review review) {
        // 현재 인증된 사용자 정보 가져오기
        User currentUser = null;
        try {
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() != null &&
                !SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser")) {
                
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                
                if (principal instanceof User) {
                    // User 타입으로 직접 변환되는 경우 (테스트 등에서 사용)
                    currentUser = (User) principal;
                } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    // UserDetails 타입인 경우 (일반적인 경우)
                    String username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                    currentUser = userRepository.findByUsername(username).orElse(null);
                }
            }
        } catch (Exception e) {
            // 인증 정보 가져오기 실패 시 로그만 남기고 계속 진행
            System.out.println("인증 정보 가져오기 실패: " + e.getMessage());
        }

        // 현재 사용자가 해당 리뷰에 좋아요/싫어요를 눌렀는지 확인
        boolean isLiked = false;
        boolean isDisliked = false;

        if (currentUser != null) {
            Long currentUserId = currentUser.getId();
            
            // 디버깅 로그 추가
            System.out.println("현재 사용자 ID: " + currentUserId);
            System.out.println("리뷰 ID: " + review.getId());
            
            isLiked = review.getLikes().stream()
                    .anyMatch(like -> like.getUser().getId().equals(currentUserId));
            
            isDisliked = review.getDislikes().stream()
                    .anyMatch(dislike -> dislike.getUser().getId().equals(currentUserId));
                
            System.out.println("좋아요 상태: " + isLiked + ", 싫어요 상태: " + isDisliked);
        } else {
            System.out.println("현재 사용자 정보를 찾을 수 없습니다.");
        }

        // 사용자 정보 가져오기 (null 체크 추가)
        User reviewUser = review.getUser();
        Long userId = null;
        String username = "Unknown User"; // 기본값 설정
        String profileImageUrl = null;

        if (reviewUser != null) {
            userId = reviewUser.getId();
            // username이 null 이거나 비어있지 않은 경우에만 실제 username 사용
            if (reviewUser.getUsername() != null && !reviewUser.getUsername().trim().isEmpty()) {
                username = reviewUser.getUsername();
            }
            profileImageUrl = reviewUser.getProfileImageUrl();
        } else {
            // review.getUser() 자체가 null인 경우 로그 추가 (문제 진단용)
            System.err.println("Warning: Review ID " + review.getId() + " has a null user associated.");
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .userId(userId) // null 가능
                .username(username) // 기본값 또는 실제 사용자 이름
                .userProfileImageUrl(profileImageUrl) // null 가능
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
                .liked(isLiked)
                .disliked(isDisliked)
                .build();
    }

    private ReviewCommentResponse convertToCommentResponse(ReviewComment comment) {
        User user = comment.getUser();
        String profileImageUrl = user.getProfileImageUrl();
        String finalProfileImageUrl = (profileImageUrl != null && !profileImageUrl.isEmpty()) 
                                       ? profileImageUrl 
                                       : "/images/default-profile.png";

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(finalProfileImageUrl)
                .bio(user.getBio())
                .build();
                
        boolean isLiked = false;
        boolean isDisliked = false;
        int likeCount = 0;
        int dislikeCount = 0;
        
        return ReviewCommentResponse.builder()
                .id(comment.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .profileImageUrl(finalProfileImageUrl)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .user(userResponse)
                .likeCount(likeCount)
                .dislikeCount(dislikeCount)
                .isLiked(isLiked)
                .isDisliked(isDisliked)
                .build();
    }
} 