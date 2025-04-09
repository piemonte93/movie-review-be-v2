package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class ProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImageUrl;
    private String bio;
    private int reviewCount;
    private int postCount;
    private List<String> roles;
    
    // 팔로워, 팔로잉 수 추가
    private long followerCount;
    private long followingCount;
    
    // 현재 로그인한 사용자가 이 프로필 사용자를 팔로우하는지 여부
    private boolean isFollowing;
    // 이 프로필 사용자가 현재 로그인한 사용자를 팔로우하는지 여부
    private boolean followsMe;
    // 서로 팔로우 하는지 여부
    private boolean mutualFollow;
} 