package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private Long postId;
    private UserSummary user;
    private int likeCount;
    private int dislikeCount;
    private int commentCount;
    private boolean liked;
    private boolean disliked;
    private Set<UserSummary> mentions;
    private List<CommentResponse> comments;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String username;
        private String profileImageUrl;
    }
} 