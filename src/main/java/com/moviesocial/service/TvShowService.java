package com.moviesocial.service;

import com.moviesocial.model.TvShow;
import com.moviesocial.repository.TvShowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TvShowService {
    private final TvShowRepository tvShowRepository;
    private final RestTemplate restTemplate;

    @Value("${tmdb.api.key}")
    private String tmdbApiKey;

    @Value("${tmdb.api.base-url}")
    private String tmdbBaseUrl;

    public Page<TvShow> getFilteredTvShows(
            List<Integer> genres,
            Integer year,
            String sortBy,
            int page,
            String query,
            Double voteMin,
            Boolean isKorean,
            Boolean isForeign,
            String network
    ) {
        PageRequest pageRequest = PageRequest.of(page - 1, 20, createSort(sortBy));
        return tvShowRepository.findByFilters(genres, year, query, voteMin, isKorean, isForeign, network, pageRequest);
    }

    public Map<String, Object> searchTvShows(String query, int page) {
        String url = String.format("%s/search/tv?api_key=%s&query=%s&page=%d&language=ko-KR",
                tmdbBaseUrl, tmdbApiKey, query, page);
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> getPopularTvShows(int page) {
        String url = String.format("%s/tv/popular?api_key=%s&page=%d&language=ko-KR",
                tmdbBaseUrl, tmdbApiKey, page);
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> getTopRatedTvShows(int page) {
        String url = String.format("%s/tv/top_rated?api_key=%s&page=%d&language=ko-KR",
                tmdbBaseUrl, tmdbApiKey, page);
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> getTvShowDetails(Long tvId) {
        String url = String.format("%s/tv/%d?api_key=%s&language=ko-KR",
                tmdbBaseUrl, tvId, tmdbApiKey);
        return restTemplate.getForObject(url, Map.class);
    }

    private Sort createSort(String sortBy) {
        return switch (sortBy) {
            case "popularity.desc" -> Sort.by(Sort.Direction.DESC, "voteCount");
            case "vote_average.desc" -> Sort.by(Sort.Direction.DESC, "voteAverage");
            case "first_air_date.desc" -> Sort.by(Sort.Direction.DESC, "firstAirDate");
            case "name.asc" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.DESC, "voteCount");
        };
    }
} 