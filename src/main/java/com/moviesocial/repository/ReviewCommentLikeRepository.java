package com.moviesocial.repository;

import com.moviesocial.model.ReviewComment;
import com.moviesocial.model.ReviewCommentLike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentLikeRepository extends JpaRepository<ReviewCommentLike, Long> {
    boolean existsByCommentAndUser(ReviewComment comment, User user);
    void deleteByCommentAndUser(ReviewComment comment, User user);
    long countByComment(ReviewComment comment);
} 