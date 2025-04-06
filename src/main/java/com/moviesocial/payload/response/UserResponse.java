package com.moviesocial.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImageUrl;
    private String bio;
    private boolean socialLogin;
    private List<String> roles;
    private String status;
    private String blockReason;
    private LocalDateTime blockDate;
} 