package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String type;
    private LocalDateTime createdAt;
    private boolean read;
    private UserSummary fromUser;
    private Long postId;
    private String postTitle;
    private Long commentId;
    private String commentContent;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String username;
        private String profileImageUrl;
    }
} 