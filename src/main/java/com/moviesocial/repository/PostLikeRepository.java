package com.moviesocial.repository;

import com.moviesocial.model.Post;
import com.moviesocial.model.PostLike;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    
    boolean existsByPostAndUser(Post post, User user);
    
    long countByPost(Post post);
    
    void deleteByPostAndUser(Post post, User user);
} 