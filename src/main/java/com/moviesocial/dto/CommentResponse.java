package com.moviesocial.dto;

import com.moviesocial.model.Comment;
import lombok.Data;

@Data
public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private String profileImageUrl;
    private Long postId;
    private String createdAt;
    
    private Comment commentEntity;
} 