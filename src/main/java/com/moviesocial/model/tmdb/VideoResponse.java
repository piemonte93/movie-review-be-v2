package com.moviesocial.model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoResponse {
    private Long id;
    private List<Video> results;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Video {
        private String id;
        private String name;
        private String key;
        private String site;
        private String type;

        @JsonProperty("iso_639_1")
        private String iso6391;

        @JsonProperty("iso_3166_1")
        private String iso31661;

        @JsonProperty("published_at")
        private String publishedAt;

        private boolean official;
    }
}