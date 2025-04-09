package com.moviesocial.payload.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userProfileImageUrl;
    private Long movieId;
    private String title;
    private String movieTitle;
    private String moviePosterPath;
    private String content;
    private Double rating;
    private Boolean isSpoiler;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String contentType;
    private Boolean liked;
    private Boolean disliked;

    public Boolean isLiked() {
        return liked;
    }

    public Boolean isDisliked() {
        return disliked;
    }
} 