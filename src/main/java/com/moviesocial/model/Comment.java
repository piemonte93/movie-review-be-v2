package com.moviesocial.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "comments")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    private Post post;
    
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CommentLike> likes = new HashSet<>();
    
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CommentDislike> dislikes = new HashSet<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // 좋아요 갯수를 반환하는 메서드
    public int getLikeCount() {
        // 컬렉션이 초기화되지 않은 경우를 위한 디버깅 로그
        if (likes == null) {
            System.out.println("경고: 댓글 likes 컬렉션이 null입니다.");
            return 0;
        }
        
        try {
            // Hibernate.initialize를 사용하여 명시적으로 컬렉션 초기화
            Hibernate.initialize(likes);
            System.out.println("댓글 getLikeCount 호출 - 좋아요 컬렉션 크기: " + likes.size());
            return likes.size();
        } catch (Exception e) {
            System.out.println("댓글 getLikeCount 예외 발생: " + e.getMessage());
            return 0;
        }
    }
    
    // 싫어요 갯수를 반환하는 메서드
    public int getDislikeCount() {
        // 컬렉션이 초기화되지 않은 경우를 위한 디버깅 로그
        if (dislikes == null) {
            System.out.println("경고: 댓글 dislikes 컬렉션이 null입니다.");
            return 0;
        }
        
        try {
            // Hibernate.initialize를 사용하여 명시적으로 컬렉션 초기화
            Hibernate.initialize(dislikes);
            System.out.println("댓글 getDislikeCount 호출 - 싫어요 컬렉션 크기: " + dislikes.size());
            return dislikes.size();
        } catch (Exception e) {
            System.out.println("댓글 getDislikeCount 예외 발생: " + e.getMessage());
            return 0;
        }
    }
} 