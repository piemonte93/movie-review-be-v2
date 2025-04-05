package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 팔로우 토글 API 응답을 위한 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollowResponse {
    private boolean isFollowing;
    private long followerCount;
    private long followingCount;
} 