package com.moviesocial.controller;

import com.moviesocial.model.TvShow;
import com.moviesocial.service.TvShowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tv")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class TvShowController {
    private final TvShowService tvShowService;

    @GetMapping
    public ResponseEntity<Page<TvShow>> getTvShows(
            @RequestParam(required = false) List<Integer> genres,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "popularity.desc") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double voteMin,
            @RequestParam(required = false) Boolean isKorean,
            @RequestParam(required = false) Boolean isForeign,
            @RequestParam(required = false) String network
    ) {
        return ResponseEntity.ok(tvShowService.getFilteredTvShows(
                genres,
                year,
                sortBy,
                page,
                query,
                voteMin,
                isKorean,
                isForeign,
                network
        ));
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchTvShows(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(tvShowService.searchTvShows(query, page));
    }

    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularTvShows(
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(tvShowService.getPopularTvShows(page));
    }

    @GetMapping("/top-rated")
    public ResponseEntity<Map<String, Object>> getTopRatedTvShows(
            @RequestParam(defaultValue = "1") int page
    ) {
        return ResponseEntity.ok(tvShowService.getTopRatedTvShows(page));
    }

    @GetMapping("/{tvId}")
    public ResponseEntity<Map<String, Object>> getTvShowDetails(@PathVariable Long tvId) {
        return ResponseEntity.ok(tvShowService.getTvShowDetails(tvId));
    }
} 