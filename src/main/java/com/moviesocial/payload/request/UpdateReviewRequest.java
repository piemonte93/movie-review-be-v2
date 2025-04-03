package com.moviesocial.payload.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateReviewRequest {
    private String title;
    private String content;
    private Double rating;
    private Boolean is_spoiler;
    private Long movie_id;
    private String movie_title;
    private String movie_poster_path;
} 