package com.moviesocial.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 사용자 팔로우 관계를 나타내는 엔티티
 */
@Entity
@Table(name = "user_following")
@Getter
@Setter
@NoArgsConstructor
public class UserFollow {
    
    @EmbeddedId
    private UserFollowId id;
    
    @MapsId("followerId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower; // 팔로우 하는 사용자
    
    @MapsId("followingId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id", nullable = false)
    private User following; // 팔로우 받는 사용자
    
    public UserFollow(User follower, User following) {
        this.id = new UserFollowId(follower.getId(), following.getId());
        this.follower = follower;
        this.following = following;
    }
} 