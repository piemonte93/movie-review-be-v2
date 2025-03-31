package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.ReviewDislike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewDislikeRepository extends JpaRepository<ReviewDislike, Long> {
    
    Optional<ReviewDislike> findByReviewAndUser(Review review, User user);
    
    boolean existsByReviewAndUser(Review review, User user);
    
    long countByReview(Review review);
    
    void deleteByReviewAndUser(Review review, User user);
} 