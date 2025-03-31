package com.moviesocial.payload.response;

import lombok.Data;

@Data
public class UserResponse {
    private Long userId;
    private String username;
    private String profileUrl;
} 