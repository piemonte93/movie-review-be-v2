package com.moviesocial.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.moviesocial.model.User;
import com.moviesocial.payload.request.UserStatusUpdateRequest;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.UserResponse;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.AdminService;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AdminService adminService;
    
    /**
     * 전체 사용자 목록 조회 API (관리자용)
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<User> users = userRepository.findAll(pageable);
        
        // User 엔티티를 UserResponse로 변환
        Page<UserResponse> userResponses = users.map(user -> {
            UserResponse response = new UserResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setProfileImageUrl(user.getProfileImageUrl());
            response.setBio(user.getBio());
            response.setSocialLogin(user.isSocialLogin());
            response.setRoles(user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList()));
            response.setStatus(user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
            response.setBlockReason(user.getBlockReason());
            response.setBlockDate(user.getBlockDate());
            return response;
        });
        
        return ResponseEntity.ok(userResponses);
    }
    
    /**
     * 사용자 상태 업데이트 API (차단/차단해제)
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateRequest request) {
        
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
            
            // 관리자 권한이 있는 사용자는 차단 불가
            boolean isAdmin = user.getRoles().stream()
                    .anyMatch(role -> role.getName().name().equals("ROLE_ADMIN"));
            
            if (isAdmin && "BLOCKED".equals(request.getStatus())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("관리자 권한을 가진, 사용자는 차단할 수 없습니다."));
            }
            
            // 상태 업데이트
            if ("BLOCKED".equals(request.getStatus())) {
                // 차단 처리
                user.setStatus(User.UserStatus.BLOCKED);
                user.setBlockReason(request.getReason());
                user.setBlockDate(LocalDateTime.now());
            } else if ("ACTIVE".equals(request.getStatus())) {
                // 차단 해제
                user.setStatus(User.UserStatus.ACTIVE);
                user.setBlockReason(null);
                user.setBlockDate(null);
            } else {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("유효하지 않은 상태값입니다. ACTIVE 또는 BLOCKED를 사용하세요."));
            }
            
            userRepository.save(user);
            
            // 응답 생성
            UserResponse response = new UserResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setProfileImageUrl(user.getProfileImageUrl());
            response.setBio(user.getBio());
            response.setSocialLogin(user.isSocialLogin());
            response.setRoles(user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList()));
            response.setStatus(user.getStatus() != null ? user.getStatus().name() : "ACTIVE");
            response.setBlockReason(user.getBlockReason());
            response.setBlockDate(user.getBlockDate());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("사용자 상태 업데이트 실패: " + e.getMessage()));
        }
    }
} 