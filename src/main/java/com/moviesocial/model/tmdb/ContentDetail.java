package com.moviesocial.model.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentDetail {

    private Long id;
    private String title;
    private String name;
    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private List<Genre> genres;
    private boolean adult;

    @JsonProperty("original_language")
    private String originalLanguage;

    @JsonProperty("original_title")
    private String originalTitle;

    @JsonProperty("original_name")
    private String originalName;

    private Double popularity;
    private boolean video;

    private Integer runtime;
    private String status;

    @JsonProperty("tagline")
    private String tagline;

    @JsonProperty("imdb_id")
    private String imdbId;

    @JsonProperty("belongs_to_collection")
    private Object belongsToCollection;

    private Long budget;
    private Long revenue;

    @JsonProperty("number_of_seasons")
    private Integer numberOfSeasons;

    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;

    @JsonProperty("production_companies")
    private List<ProductionCompany> productionCompanies;

    @JsonProperty("production_countries")
    private List<ProductionCountry> productionCountries;

    @JsonProperty("spoken_languages")
    private List<SpokenLanguage> spokenLanguages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Genre {
        private Long id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionCompany {
        private Long id;
        private String name;

        @JsonProperty("logo_path")
        private String logoPath;

        @JsonProperty("origin_country")
        private String originCountry;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionCountry {
        @JsonProperty("iso_3166_1")
        private String iso31661;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpokenLanguage {
        @JsonProperty("iso_639_1")
        private String iso6391;
        private String name;

        @JsonProperty("english_name")
        private String englishName;
    }
}