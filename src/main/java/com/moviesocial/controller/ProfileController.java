package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.service.ProfileService;
import com.moviesocial.service.ReviewService;
import com.moviesocial.service.ScrapService;
import com.moviesocial.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable String username) {
        ProfileResponse profile = profileService.getUserProfile(username);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ProfileResponse> getUserProfileById(@PathVariable Long id) {
        // ID로 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + id));
        
        // 유저명으로 프로필 정보 조회
        ProfileResponse profile = profileService.getUserProfile(user.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponse> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        ProfileResponse profile = profileService.getUserProfile(username);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{username}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String contentType) {
        if (contentType != null && !contentType.isEmpty()) {
            return ResponseEntity.ok(reviewService.getUserReviews(username, page, size, contentType));
        } else {
            return ResponseEntity.ok(reviewService.getUserReviews(username, page, size));
        }
    }

    @GetMapping("/me/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        return ResponseEntity.ok(reviewService.getUserReviews(username, page, size));
    }

    @GetMapping("/{username}/scraps")
    public ResponseEntity<?> getUserScraps(@PathVariable String username) {
        try {
            // 유저 확인 로직 제거 - 접근 제한 완화
            return ResponseEntity.ok(scrapService.getUserScraps(username));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("스크랩 목록을 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{username}/activity")
    public ResponseEntity<?> getUserActivity(@PathVariable String username) {
        try {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("활동 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
} 