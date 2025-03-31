package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCommentResponse {
    private Long id;
    private Long reviewId;
    private Long userId;
    private String username;
    private String userProfileUrl;
    private String content;
    private Integer likeCount;
    private Integer dislikeCount;
    private Boolean isLiked;
    private Boolean isDisliked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 