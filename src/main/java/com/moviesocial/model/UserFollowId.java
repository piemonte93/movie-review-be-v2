package com.moviesocial.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * UserFollow 엔티티의 복합 기본키를 위한 임베디드 ID 클래스
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowId implements Serializable {
    @Column(name = "follower_id")
    private Long followerId;
    
    @Column(name = "following_id")
    private Long followingId;
} 