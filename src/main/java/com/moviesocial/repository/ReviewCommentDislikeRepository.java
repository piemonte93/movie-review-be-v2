package com.moviesocial.repository;

import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.ReviewCommentDislike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentDislikeRepository extends JpaRepository<ReviewCommentDislike, Long> {
    boolean existsByCommentAndUser(ReviewComment comment, User user);
    void deleteByCommentAndUser(ReviewComment comment, User user);
    long countByComment(ReviewComment comment);
} 