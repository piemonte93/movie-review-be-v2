package com.moviesocial.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @JsonIgnore
    private Post post;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
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
            int count = likes.size();
            System.out.println("댓글 getLikeCount 호출 - likes.size() 호출 결과: " + count);
            return count;
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
            int count = dislikes.size();
            System.out.println("댓글 getDislikeCount 호출 - dislikes.size() 호출 결과: " + count);
            return count;
        } catch (Exception e) {
            System.out.println("댓글 getDislikeCount 예외 발생: " + e.getMessage());
            return 0;
        }
    }
} 