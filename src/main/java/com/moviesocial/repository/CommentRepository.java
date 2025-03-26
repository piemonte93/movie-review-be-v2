package com.moviesocial.repository;

import com.moviesocial.model.Comment;
import com.moviesocial.model.Post;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);
    
    Page<Comment> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    long countByPost(Post post);
} 