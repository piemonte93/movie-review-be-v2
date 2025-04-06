package com.moviesocial.service;

import com.moviesocial.payload.response.FollowUserResponse;
import com.moviesocial.payload.response.UserFollowResponse;

import java.util.List;

/**
 * 사용자 팔로우 기능 서비스 인터페이스
 */
public interface UserFollowService {

    /**
     * 팔로우/언팔로우 토글
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param targetUserId 팔로우/언팔로우 대상 사용자 ID
     * @return 팔로우 상태 및 카운트 정보
     */
    UserFollowResponse toggleFollow(Long currentUserId, Long targetUserId);
    
    /**
     * 특정 사용자의 팔로워 목록 조회
     * @param userId 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 팔로워 목록
     */
    List<FollowUserResponse> getFollowers(Long userId, Long currentUserId);
    
    /**
     * 특정 사용자가 팔로우하는 사용자 목록 조회
     * @param userId 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 팔로잉 목록
     */
    List<FollowUserResponse> getFollowing(Long userId, Long currentUserId);
    
    /**
     * 현재 로그인한 사용자의 팔로워 목록 조회
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 팔로워 목록
     */
    List<FollowUserResponse> getMyFollowers(Long currentUserId);
    
    /**
     * 현재 로그인한 사용자가 팔로우하는 사용자 목록 조회
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 팔로잉 목록
     */
    List<FollowUserResponse> getMyFollowing(Long currentUserId);
    
    /**
     * 두 사용자 간의 팔로우 관계 확인
     * @param followerId 팔로워 ID
     * @param followingId 팔로잉 ID
     * @return 팔로우 여부
     */
    boolean isFollowing(Long followerId, Long followingId);
} 