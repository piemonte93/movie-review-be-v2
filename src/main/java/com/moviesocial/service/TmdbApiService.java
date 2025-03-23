package com.moviesocial.service;

import com.moviesocial.model.tmdb.MovieResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class TmdbApiService {

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.api.base-url}")
    private String baseUrl;

    @Value("${tmdb.api.image-url}")
    private String imageUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public TmdbApiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public MovieResponse getTrendingMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/trending/movie/week")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, MovieResponse.class);
    }

    public MovieResponse getTrendingAll() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/trending/all/week")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, MovieResponse.class);
    }

    public MovieResponse getTopRatedMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/top_rated")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, MovieResponse.class);
    }

    public MovieResponse getUpcomingMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/upcoming")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, MovieResponse.class);
    }

    public MovieResponse getNowPlayingMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/now_playing")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, MovieResponse.class);
    }

    public String getImageUrl(String path, String size) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return imageUrl + size + path;
    }
}
