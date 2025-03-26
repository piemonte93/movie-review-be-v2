package com.moviesocial.controller;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.ProfileUpdateRequest;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.security.jwt.JwtUtils;
import com.moviesocial.service.ProfileService;
import com.moviesocial.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    /**
     * 사용자 프로필 정보를 조회하는 API
     * @param username 사용자명
     * @return 사용자 프로필 정보
     */
    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getUserProfile(@PathVariable String username) {
        ProfileResponse profile = profileService.getUserProfile(username);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 현재 로그인한 사용자의 프로필 정보를 조회하는 API
     * @param authHeader 인증 헤더
     * @return 사용자 프로필 정보
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponse> getMyProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        ProfileResponse profile = profileService.getUserProfile(username);
        return ResponseEntity.ok(profile);
    }
    
    /**
     * 사용자 프로필 정보를 업데이트하는 API
     * @param authHeader 인증 헤더
     * @param request 업데이트할 프로필 정보
     * @return 성공 메시지
     */
    @PutMapping("/update")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authHeader, 
                                          @RequestBody ProfileUpdateRequest request) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        profileService.updateProfile(username, request);
        return ResponseEntity.ok(new MessageResponse("프로필이 성공적으로 업데이트되었습니다."));
    }
    
    /**
     * 프로필 이미지 업로드 API
     * @param authHeader 인증 헤더
     * @param file 업로드할 이미지 파일
     * @return 성공 메시지
     */
    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadProfileImage(@RequestHeader("Authorization") String authHeader,
                                               @RequestParam("file") MultipartFile file) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        String imageUrl = profileService.uploadProfileImage(username, file);
        return ResponseEntity.ok(new MessageResponse("프로필 이미지가 성공적으로 업로드되었습니다."));
    }
    
    /**
     * 사용자의 리뷰 목록을 가져오는 API
     * @param username 사용자명
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자 리뷰 목록
     */
    @GetMapping("/{username}/reviews")
    public ResponseEntity<List<ReviewResponse>> getUserReviews(@PathVariable String username,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        List<ReviewResponse> reviews = reviewService.getUserReviews(username, page, size);
        return ResponseEntity.ok(reviews);
    }
    
    /**
     * 로그인한 사용자의 리뷰 목록을 가져오는 API
     * @param authHeader 인증 헤더
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 사용자 리뷰 목록
     */
    @GetMapping("/me/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@RequestHeader("Authorization") String authHeader,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        String token = authHeader.substring(7);
        String username = jwtUtils.getUserNameFromJwtToken(token);
        List<ReviewResponse> reviews = reviewService.getUserReviews(username, page, size);
        return ResponseEntity.ok(reviews);
    }
} 