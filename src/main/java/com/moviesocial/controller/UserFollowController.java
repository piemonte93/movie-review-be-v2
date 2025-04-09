package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.FollowUserResponse;
import com.moviesocial.payload.response.UserFollowResponse;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.ScrapService;
import com.moviesocial.service.UserFollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자 팔로우 기능 컨트롤러
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserFollowController {

    private final UserFollowService userFollowService;
    private final UserRepository userRepository;
    private final ScrapService scrapService;

    /**
     * 팔로우/언팔로우 토글
     */
    @PostMapping("/follow/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserFollowResponse> toggleFollow(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("팔로우 토글 요청 - 대상 사용자: {}, 요청자: {}", userId, userDetails.getUsername());
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        
        UserFollowResponse response = userFollowService.toggleFollow(currentUser.getId(), userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 내 팔로워 목록 조회
     */
    @GetMapping("/followers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FollowUserResponse>> getMyFollowers(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 팔로워 목록 조회 요청 - 요청자: {}", userDetails.getUsername());
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        
        List<FollowUserResponse> followers = userFollowService.getMyFollowers(currentUser.getId());
        
        return ResponseEntity.ok(followers);
    }

    /**
     * 내 팔로잉 목록 조회
     */
    @GetMapping("/following")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FollowUserResponse>> getMyFollowing(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("내 팔로잉 목록 조회 요청 - 요청자: {}", userDetails.getUsername());
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        
        List<FollowUserResponse> following = userFollowService.getMyFollowing(currentUser.getId());
        
        return ResponseEntity.ok(following);
    }

    /**
     * 특정 사용자의 팔로워 목록 조회
     */
    @GetMapping("/{userId}/followers")
    public ResponseEntity<List<FollowUserResponse>> getUserFollowers(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("사용자 팔로워 목록 조회 요청 - 대상 사용자: {}, 요청자: {}", userId, userDetails.getUsername());
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        
        List<FollowUserResponse> followers = userFollowService.getFollowers(userId, currentUser.getId());
        
        return ResponseEntity.ok(followers);
    }

    /**
     * 특정 사용자의 팔로잉 목록 조회
     */
    @GetMapping("/{userId}/following")
    public ResponseEntity<List<FollowUserResponse>> getUserFollowing(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("사용자 팔로잉 목록 조회 요청 - 대상 사용자: {}, 요청자: {}", userId, userDetails.getUsername());
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        
        List<FollowUserResponse> following = userFollowService.getFollowing(userId, currentUser.getId());
        
        return ResponseEntity.ok(following);
    }

    /**
     * 팔로잉 중인 사용자들의 스크랩 목록 조회
     */
    @GetMapping("/following/scraps")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getFollowingScraps(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("팔로잉 중인 사용자들의 스크랩 목록 조회 요청 - 요청자: {}", userDetails.getUsername());
        
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));
        
        // 1. 팔로잉 사용자 ID 목록 가져오기
        List<Long> followingUserIds = userFollowService.getMyFollowing(currentUser.getId()).stream()
                .map(FollowUserResponse::getId)
                .collect(Collectors.toList());
        
        // 2. 팔로잉 사용자들의 스크랩 가져오기
        List<Map<String, Object>> scraps = scrapService.getFollowingScraps(followingUserIds);
        
        log.info("팔로잉 사용자들의 스크랩 {} 건 반환", scraps.size());
        
        return ResponseEntity.ok(scraps);
    }
} 