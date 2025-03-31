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

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable String username) {
        ProfileResponse profile = profileService.getUserProfile(username);
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