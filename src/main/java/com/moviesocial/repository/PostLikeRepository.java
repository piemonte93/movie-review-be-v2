package com.moviesocial.repository;

import com.moviesocial.model.Post;
import com.moviesocial.model.PostLike;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostAndUser(Post post, User user);
    
    boolean existsByPostAndUser(Post post, User user);
    
    long countByPost(Post post);
    
    void deleteByPostAndUser(Post post, User user);
    
    // 사용자가 좋아요 누른 게시글 목록을 페이지네이션으로 가져오는 메서드
    @Query("SELECT pl.post FROM PostLike pl WHERE pl.user = :user ORDER BY pl.createdAt DESC")
    Page<Post> findPostsByUser(@Param("user") User user, Pageable pageable);
} 