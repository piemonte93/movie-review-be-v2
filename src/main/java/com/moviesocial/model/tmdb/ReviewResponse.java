package com.moviesocial.model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Integer page;
    private List<Review> results;

    @JsonProperty("total_pages")
    private Integer totalPages;

    @JsonProperty("total_results")
    private Integer totalResults;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Review {
        private String id;
        private String author;

        @JsonProperty("author_details")
        private AuthorDetails authorDetails;

        private String content;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;

        private String url;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AuthorDetails {
            private String name;
            private String username;

            @JsonProperty("avatar_path")
            private String avatarPath;

            private Double rating;
        }
    }
}