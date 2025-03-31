package com.moviesocial.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private String content;
    private String username;
    private String profileImageUrl;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Long userId;
} 