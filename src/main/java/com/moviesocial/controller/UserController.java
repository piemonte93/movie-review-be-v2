package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.UserResponse;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * 현재 로그인한 사용자의 상태를 조회합니다.
     * ACTIVE, BLOCKED, DELETED 상태를 반환합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<?> getUserStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body("인증되지 않은 사용자입니다.");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Map<String, String> response = new HashMap<>();
        response.put("status", user.getStatus().name());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam("query") String query,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<User> userPage = userService.searchUsersByUsername(query, pageable);
        
        // Convert User Page to UserResponse Page using setters
        Page<UserResponse> responsePage = userPage.map(user -> {
            UserResponse res = new UserResponse();
            res.setId(user.getId());
            res.setUsername(user.getUsername());
            res.setEmail(user.getEmail());
            res.setProfileImageUrl(user.getProfileImageUrl());
            res.setBio(user.getBio());
            res.setSocialLogin(user.isSocialLogin());
            res.setStatus(user.getStatus() != null ? user.getStatus().name() : null);
            res.setBlockReason(user.getBlockReason());
            res.setBlockDate(user.getBlockDate());
            res.setReportedCount(user.getReportedCount());
            List<String> roles = user.getRoles().stream()
                                      .map(role -> role.getName().name())
                                      .collect(Collectors.toList());
            res.setRoles(roles); 
            return res;
        });
                
        return ResponseEntity.ok(responsePage);
    }
} 