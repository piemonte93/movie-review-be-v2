package com.moviesocial.service.impl;

import com.moviesocial.model.User;
import com.moviesocial.model.UserFollow;
import com.moviesocial.payload.response.FollowUserResponse;
import com.moviesocial.payload.response.UserFollowResponse;
import com.moviesocial.repository.UserFollowRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 팔로우 기능 서비스 구현 클래스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserFollowServiceImpl implements UserFollowService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;

    /**
     * 팔로우/언팔로우 토글
     */
    @Override
    @Transactional
    public UserFollowResponse toggleFollow(Long currentUserId, Long targetUserId) {
        log.info("팔로우 토글 - 현재 사용자: {}, 대상 사용자: {}", currentUserId, targetUserId);
        
        // 자기 자신을 팔로우하는 것을 방지
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("자기 자신을 팔로우할 수 없습니다.");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("현재 사용자를 찾을 수 없습니다."));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("대상 사용자를 찾을 수 없습니다."));

        Optional<UserFollow> existingFollow = userFollowRepository
                .findByFollowerIdAndFollowingId(currentUserId, targetUserId);

        boolean isNowFollowing;

        if (existingFollow.isPresent()) {
            // 이미 팔로우 중이면 언팔로우
            log.info("기존 팔로우 관계 삭제: {}", existingFollow.get().getId());
            userFollowRepository.delete(existingFollow.get());
            isNowFollowing = false;
        } else {
            // 팔로우 관계 생성
            UserFollow newFollow = new UserFollow(currentUser, targetUser);
            log.info("새 팔로우 관계 생성");
            userFollowRepository.save(newFollow);
            isNowFollowing = true;
        }

        // 최신 팔로워/팔로잉 수 조회
        long followerCount = userFollowRepository.countByFollowingId(targetUserId);
        long followingCount = userFollowRepository.countByFollowerId(currentUserId);
        
        log.info("팔로우 토글 완료 - 현재 상태: {}, 팔로워 수: {}, 팔로잉 수: {}", 
                isNowFollowing, followerCount, followingCount);

        return UserFollowResponse.builder()
                .isFollowing(isNowFollowing)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .build();
    }

    /**
     * 특정 사용자의 팔로워 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowers(Long userId, Long currentUserId) {
        log.info("팔로워 목록 조회 - 대상 사용자: {}, 현재 사용자: {}", userId, currentUserId);
        
        List<User> followers = userFollowRepository.findFollowersWithRolesByUserId(userId);
        
        return buildFollowUserResponseList(followers, currentUserId);
    }

    /**
     * 특정 사용자가 팔로우하는 사용자 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<FollowUserResponse> getFollowing(Long userId, Long currentUserId) {
        log.info("팔로잉 목록 조회 - 대상 사용자: {}, 현재 사용자: {}", userId, currentUserId);
        
        List<User> following = userFollowRepository.findFollowingWithRolesByUserId(userId);
        
        return buildFollowUserResponseList(following, currentUserId);
    }

    /**
     * 현재 로그인한 사용자의 팔로워 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<FollowUserResponse> getMyFollowers(Long currentUserId) {
        return getFollowers(currentUserId, currentUserId);
    }

    /**
     * 현재 로그인한 사용자가 팔로우하는 사용자 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<FollowUserResponse> getMyFollowing(Long currentUserId) {
        return getFollowing(currentUserId, currentUserId);
    }

    /**
     * 두 사용자 간의 팔로우 관계 확인
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return userFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId).isPresent();
    }

    /**
     * 사용자 목록에서 FollowUserResponse 리스트 생성
     */
    private List<FollowUserResponse> buildFollowUserResponseList(List<User> users, Long currentUserId) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }

        // 현재 사용자가 팔로우하는 사용자 목록 조회
        List<Long> followingIds = userFollowRepository.findFollowingByUserId(currentUserId)
                .stream().map(User::getId).collect(Collectors.toList());

        // 현재 사용자를 팔로우하는 사용자 목록 조회
        List<Long> followerIds = userFollowRepository.findFollowersByUserId(currentUserId)
                .stream().map(User::getId).collect(Collectors.toList());

        return users.stream()
                .map(user -> {
                    boolean isFollowing = followingIds.contains(user.getId());
                    boolean followsMe = followerIds.contains(user.getId());
                    
                    return FollowUserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .profileImageUrl(user.getProfileImageUrl())
                            .bio(user.getBio())
                            .isFollowing(isFollowing)
                            .followsMe(followsMe)
                            .mutualFollow(isFollowing && followsMe)
                            .build();
                })
                .collect(Collectors.toList());
    }
} 