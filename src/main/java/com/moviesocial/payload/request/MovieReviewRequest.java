package com.moviesocial.payload.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MovieReviewRequest {

    @NotBlank(message = "리뷰 제목은 필수입니다.")
    @Size(min = 2, max = 100, message = "제목은 2~100자 이내로 작성해주세요.")
    private String title;

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(min = 10, max = 1000, message = "내용은 10~1000자 이내로 작성해주세요.")
    private String content;

    @NotNull(message = "별점은 필수입니다.")
    @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 최대 5점까지 가능합니다.")
    private Integer rating;

    @NotNull(message = "영화 ID는 필수입니다.")
    private Long movieId;

    @NotBlank(message = "영화 제목은 필수입니다.")
    private String movieTitle;

    private String moviePoster;

    private Boolean isSpoiler = false;
} 