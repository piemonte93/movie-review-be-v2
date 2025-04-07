package com.moviesocial.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.moviesocial.model.User;
import com.moviesocial.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

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
} 