package com.moviesocial.client;

import com.moviesocial.model.tmdb.ContentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TmdbApiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    @Autowired
    public TmdbApiClient(
            RestTemplate restTemplate,
            @Value("${tmdb.api.key}") String apiKey,
            @Value("${tmdb.api.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public ContentResponse getTrendingMovies() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/trending/movie/week")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getTopRatedMovies() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/movie/top_rated")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getUpcomingMovies() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/movie/upcoming")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getNowPlayingMovies() {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/movie/now_playing")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public Object getMovieDetails(Long movieId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/movie/{id}")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .buildAndExpand(movieId)
                .toUriString();

        return restTemplate.getForObject(url, Object.class);
    }

    public Object getMovieReviews(Long movieId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/movie/{id}/reviews")
                .queryParam("api_key", apiKey)
                .buildAndExpand(movieId)
                .toUriString();

        return restTemplate.getForObject(url, Object.class);
    }
}
