package com.moviesocial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 사용자 팔로우 관계를 나타내는 엔티티
 */
@Entity
@Table(name = "user_follows", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"follower_id", "following_id"})
       })
@Getter
@Setter
@NoArgsConstructor
public class UserFollow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower; // 팔로우 하는 사용자
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following; // 팔로우 받는 사용자
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public UserFollow(User follower, User following) {
        this.follower = follower;
        this.following = following;
    }
} 