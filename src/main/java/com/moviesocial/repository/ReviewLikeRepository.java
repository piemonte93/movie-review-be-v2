package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewLike;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    
    Optional<ReviewLike> findByReviewAndUser(Review review, User user);
    
    boolean existsByReviewAndUser(Review review, User user);
    
    long countByReview(Review review);
    
    void deleteByReviewAndUser(Review review, User user);
    
    // 사용자가 좋아요 누른 리뷰 목록을 페이지네이션으로 가져오는 메서드
    @Query("SELECT rl.review FROM ReviewLike rl WHERE rl.user = :user ORDER BY rl.createdAt DESC")
    Page<Review> findReviewsByUser(@Param("user") User user, Pageable pageable);
} 