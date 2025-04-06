package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팔로워/팔로잉 목록 조회 API 응답을 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUserResponse {
    private Long id;
    private String username;
    private String profileImageUrl;
    private String bio;
    private boolean isFollowing;
    private boolean followsMe;
    private boolean mutualFollow;
} 