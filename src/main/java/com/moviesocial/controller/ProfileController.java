package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.service.ProfileService;
import com.moviesocial.service.ReviewService;
import com.moviesocial.service.ScrapService;
import com.moviesocial.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moviesocial.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScrapService scrapService;

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getUserProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("사용자 프로필 조회 요청 - 대상: {}", username);
        
        if (userDetails == null) {
            // 로그인하지 않은 사용자는 기본 프로필 정보만 조회
            return ResponseEntity.ok(profileService.getUserProfile(username));
        }
        
        // 팔로우 상태를 포함한 프로필 정보 반환
        ProfileResponse profile = profileService.getUserProfileWithFollowStatus(username, userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ProfileResponse> getUserProfileById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("ID로 사용자 프로필 조회 요청 - 대상 ID: {}", id);
        
        // ID로 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + id));
        
        if (userDetails == null) {
            // 로그인하지 않은 사용자는 기본 프로필 정보만 조회
            return ResponseEntity.ok(profileService.getUserProfile(user.getUsername()));
        }
        
        // 팔로우 상태를 포함한 프로필 정보 반환
        ProfileResponse profile = profileService.getUserProfileByIdWithFollowStatus(id, userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        log.info("내 프로필 조회 요청 - 사용자: {}", userDetails.getUsername());
        
        // 자신의 프로필 정보는 팔로우 상태 포함 필요 없음
        ProfileResponse profile = profileService.getUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{username}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String contentType) {
        
        log.info("사용자 리뷰 조회 요청 - 대상: {}, 페이지: {}, 사이즈: {}, 콘텐츠 타입: {}", 
                username, page, size, contentType);
        
        if (contentType != null && !contentType.isEmpty()) {
            return ResponseEntity.ok(reviewService.getUserReviews(username, page, size, contentType));
        } else {
            return ResponseEntity.ok(reviewService.getUserReviews(username, page, size));
        }
    }

    @GetMapping("/me/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("내 리뷰 조회 요청 - 사용자: {}, 페이지: {}, 사이즈: {}", 
                userDetails.getUsername(), page, size);
        
        return ResponseEntity.ok(reviewService.getUserReviews(userDetails.getUsername(), page, size));
    }

    @GetMapping("/{username}/scraps")
    public ResponseEntity<?> getUserScraps(@PathVariable String username) {
        try {
            log.info("사용자 스크랩 조회 요청 - 대상: {}", username);
            
            // 유저 확인 로직 제거 - 접근 제한 완화
            return ResponseEntity.ok(scrapService.getUserScraps(username));
        } catch (Exception e) {
            log.error("스크랩 목록 조회 실패 - 대상: {}, 오류: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("스크랩 목록을 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{username}/activity")
    public ResponseEntity<?> getUserActivity(@PathVariable String username) {
        try {
            log.info("사용자 활동 조회 요청 - 대상: {}", username);
            
            // 사용자가 존재하는지 확인
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다: " + username));
            
            // 더미 활동 데이터 반환 (실제 구현 필요)
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("favoriteMovies", new ArrayList<>());
            activityData.put("favoriteReviews", new ArrayList<>());
            activityData.put("favoritePosts", new ArrayList<>());
            
            return ResponseEntity.ok(activityData);
        } catch (Exception e) {
            log.error("활동 정보 조회 실패 - 대상: {}, 오류: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("활동 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 