package com.moviesocial.repository;

import com.moviesocial.model.Comment;
import com.moviesocial.model.CommentDislike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentDislikeRepository extends JpaRepository<CommentDislike, Long> {
    Optional<CommentDislike> findByCommentAndUser(Comment comment, User user);
    
    boolean existsByCommentAndUser(Comment comment, User user);
    
    long countByComment(Comment comment);
    
    void deleteByCommentAndUser(Comment comment, User user);
} 