package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    
    Page<ReviewComment> findByReview(Review review, Pageable pageable);
    
    List<ReviewComment> findByReviewOrderByCreatedAtDesc(Review review);
    
    Optional<ReviewComment> findByIdAndUser(Long id, User user);
    
    long countByReview(Review review);
    
    @Query("SELECT c FROM ReviewComment c JOIN FETCH c.user WHERE c.review.id = :reviewId ORDER BY c.createdAt DESC")
    List<ReviewComment> findByReviewId(Long reviewId);
    
    List<ReviewComment> findByReviewIdOrderByCreatedAtDesc(Long reviewId);

    @Query("SELECT rc FROM ReviewComment rc " +
           "JOIN FETCH rc.user " +
           "JOIN FETCH rc.review " +
           "WHERE rc.review.id = :reviewId " +
           "ORDER BY rc.createdAt DESC")
    List<ReviewComment> findByReviewIdWithDetails(@Param("reviewId") Long reviewId);
} 