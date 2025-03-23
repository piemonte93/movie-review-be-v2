package com.moviesocial.service;

import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.model.tmdb.ContentResponse;
import com.moviesocial.model.tmdb.ReviewResponse;
import com.moviesocial.model.tmdb.VideoResponse;
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

    public ContentResponse getTrendingMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/trending/movie/week")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getTrendingAll() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/trending/all/week")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getTopRatedMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/top_rated")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getUpcomingMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/upcoming")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    public ContentResponse getNowPlayingMovies() {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/now_playing")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("region", "KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    // 영화 상세 정보를 가져오는 메서드
    public ContentDetail getMovieDetails(Long movieId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentDetail.class);
    }

    // 영화 비디오 목록을 가져오는 메서드
    public VideoResponse getMovieVideos(Long movieId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/" + movieId + "/videos")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        VideoResponse response = restTemplate.getForObject(url, VideoResponse.class);

        // 한국어 비디오가 없거나 부족한 경우 영어 비디오도 가져옴
        if (response != null && (response.getResults() == null || response.getResults().size() < 2)) {
            String urlEn = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/movie/" + movieId + "/videos")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en-US")  // 영어 비디오
                    .build()
                    .toUriString();

            VideoResponse enResponse = restTemplate.getForObject(urlEn, VideoResponse.class);
            if (enResponse != null && enResponse.getResults() != null && !enResponse.getResults().isEmpty()) {
                // 영어 비디오가 있으면 한국어 비디오 목록에 추가
                if (response.getResults() == null) {
                    response.setResults(enResponse.getResults());
                } else {
                    response.getResults().addAll(enResponse.getResults());
                }
            }
        }

        return response;
    }

    // 영화 리뷰 목록을 가져오는 메서드
    public ReviewResponse getMovieReviews(Long movieId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/" + movieId + "/reviews")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        ReviewResponse response = restTemplate.getForObject(url, ReviewResponse.class);

        // 한국어 리뷰가 없는 경우 영어 리뷰도 가져옴
        if (response != null && (response.getResults() == null || response.getResults().isEmpty())) {
            String urlEn = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/movie/" + movieId + "/reviews")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en-US")  // 영어 리뷰
                    .build()
                    .toUriString();

            return restTemplate.getForObject(urlEn, ReviewResponse.class);
        }

        return response;
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

    // TV 프로그램 상세 정보를 가져오는 메서드
    public ContentDetail getTvDetails(Long tvId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/tv/" + tvId)
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentDetail.class);
    }

    // TV 프로그램 비디오 목록을 가져오는 메서드
    public VideoResponse getTvVideos(Long tvId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/tv/" + tvId + "/videos")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        VideoResponse response = restTemplate.getForObject(url, VideoResponse.class);

        // 한국어 비디오가 없거나 부족한 경우 영어 비디오도 가져옴
        if (response != null && (response.getResults() == null || response.getResults().size() < 2)) {
            String urlEn = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/tv/" + tvId + "/videos")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en-US")  // 영어 비디오
                    .build()
                    .toUriString();

            VideoResponse enResponse = restTemplate.getForObject(urlEn, VideoResponse.class);
            if (enResponse != null && enResponse.getResults() != null && !enResponse.getResults().isEmpty()) {
                // 영어 비디오가 있으면 한국어 비디오 목록에 추가
                if (response.getResults() == null) {
                    response.setResults(enResponse.getResults());
                } else {
                    response.getResults().addAll(enResponse.getResults());
                }
            }
        }

        return response;
    }

    // TV 프로그램 리뷰 목록을 가져오는 메서드
    public ReviewResponse getTvReviews(Long tvId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/tv/" + tvId + "/reviews")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        ReviewResponse response = restTemplate.getForObject(url, ReviewResponse.class);

        // 한국어 리뷰가 없는 경우 영어 리뷰도 가져옴
        if (response != null && (response.getResults() == null || response.getResults().isEmpty())) {
            String urlEn = UriComponentsBuilder
                    .fromHttpUrl(baseUrl + "/tv/" + tvId + "/reviews")
                    .queryParam("api_key", apiKey)
                    .queryParam("language", "en-US")  // 영어 리뷰
                    .build()
                    .toUriString();

            return restTemplate.getForObject(urlEn, ReviewResponse.class);
        }

        return response;
    }
}