package com.moviesocial.repository;

import com.moviesocial.model.Post;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<Post> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:query% ORDER BY p.createdAt DESC")
    Page<Post> searchByTitle(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.content LIKE %:query% ORDER BY p.createdAt DESC")
    Page<Post> searchByContent(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.user.username LIKE %:query% ORDER BY p.createdAt DESC")
    Page<Post> searchByAuthor(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Post p JOIN p.mentions m WHERE m.id = :userId ORDER BY p.createdAt DESC")
    List<Post> findPostsMentioningUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(pl) FROM PostLike pl WHERE pl.post.id = :postId")
    long countLikesByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(pd) FROM PostDislike pd WHERE pd.post.id = :postId")
    long countDislikesByPostId(@Param("postId") Long postId);

    long countByUserId(Long userId);
} 