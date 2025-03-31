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
public class MovieReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userProfileUrl;
    private String title;
    private String content;
    private Integer rating;
    private Long movieId;
    private String movieTitle;
    private String moviePoster;
    private Boolean isSpoiler;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private Boolean isLiked;
    private Boolean isDisliked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 