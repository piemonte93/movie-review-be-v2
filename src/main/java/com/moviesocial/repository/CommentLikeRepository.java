package com.moviesocial.repository;

import com.moviesocial.model.Comment;
import com.moviesocial.model.CommentLike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);
    
    boolean existsByCommentAndUser(Comment comment, User user);
    
    long countByComment(Comment comment);
    
    void deleteByCommentAndUser(Comment comment, User user);
} 