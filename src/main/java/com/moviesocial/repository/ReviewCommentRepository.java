package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.User;
import com.moviesocial.payload.response.ReviewCommentResponse;
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
    
    // Page<ReviewComment> findByReview(Review review, Pageable pageable);
    
    @Query(value = "SELECT new com.moviesocial.payload.response.ReviewCommentResponse(" +
                   "rc.id, rc.user.id, rc.user.username, " +
                   "COALESCE(rc.user.profileImageUrl, '/images/default-profile.png'), " +
                   "rc.content, rc.createdAt, rc.updatedAt, " +
                   "null, " +
                   "0, 0, false, false) " +
                   "FROM ReviewComment rc JOIN rc.user u " +
                   "WHERE rc.review = :review",
           countQuery = "SELECT count(rc) FROM ReviewComment rc WHERE rc.review = :review")
    Page<ReviewCommentResponse> findReviewCommentsByReview(@Param("review") Review review, Pageable pageable);
    
    @Query("SELECT rc FROM ReviewComment rc JOIN FETCH rc.user WHERE rc.review = :review ORDER BY rc.createdAt DESC")
    List<ReviewComment> findByReviewOrderByCreatedAtDescWithUser(@Param("review") Review review);
    
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