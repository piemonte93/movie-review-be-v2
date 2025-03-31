package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewLike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    
    Optional<ReviewLike> findByReviewAndUser(Review review, User user);
    
    boolean existsByReviewAndUser(Review review, User user);
    
    long countByReview(Review review);
    
    void deleteByReviewAndUser(Review review, User user);
} 