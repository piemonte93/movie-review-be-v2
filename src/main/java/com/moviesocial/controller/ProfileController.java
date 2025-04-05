package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.service.ProfileService;
import com.moviesocial.service.ReviewService;
import com.moviesocial.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moviesocial.repository.UserRepository;

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
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(reviewService.getUserReviews(username, page, size));
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
} 