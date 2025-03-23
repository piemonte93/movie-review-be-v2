package com.moviesocial.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TmdbConfig {

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Value("${tmdb.api.image-url}")
    private String imageUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTrendingMoviesUrl() {
        return baseUrl + "/trending/movie/week?api_key=" + apiKey;
    }

    public String getTopRatedMoviesUrl() {
        return baseUrl + "/movie/top_rated?api_key=" + apiKey;
    }

    public String getUpcomingMoviesUrl() {
        return baseUrl + "/movie/upcoming?api_key=" + apiKey;
    }

    public String getNowPlayingMoviesUrl() {
        return baseUrl + "/movie/now_playing?api_key=" + apiKey;
    }

    public String getMovieDetailsUrl(Long movieId) {
        return baseUrl + "/movie/" + movieId + "?api_key=" + apiKey;
    }

    public String getMovieReviewsUrl(Long movieId) {
        return baseUrl + "/movie/" + movieId + "/reviews?api_key=" + apiKey;
    }

    public String getMovieCreditsUrl(Long movieId) {
        return baseUrl + "/movie/" + movieId + "/credits?api_key=" + apiKey;
    }

    public String getImageFullUrl(String posterPath, String size) {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        return imageUrl + size + posterPath;
    }
}
