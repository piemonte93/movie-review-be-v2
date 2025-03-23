package com.moviesocial.controller;

import com.moviesocial.model.tmdb.MovieResponse;
import com.moviesocial.service.TmdbApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final TmdbApiService tmdbApiService;

    @Autowired
    public MovieController(TmdbApiService tmdbApiService) {
        this.tmdbApiService = tmdbApiService;
    }

    @GetMapping("/trending")
    public ResponseEntity<MovieResponse> getTrendingMovies() {
        return ResponseEntity.ok(tmdbApiService.getTrendingMovies());
    }

    @GetMapping("/trending-all")
    public ResponseEntity<MovieResponse> getTrendingAll() {
        return ResponseEntity.ok(tmdbApiService.getTrendingAll());
    }

    @GetMapping("/top-rated")
    public ResponseEntity<MovieResponse> getTopRatedMovies() {
        return ResponseEntity.ok(tmdbApiService.getTopRatedMovies());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<MovieResponse> getUpcomingMovies() {
        return ResponseEntity.ok(tmdbApiService.getUpcomingMovies());
    }

    @GetMapping("/now-playing")
    public ResponseEntity<MovieResponse> getNowPlayingMovies() {
        return ResponseEntity.ok(tmdbApiService.getNowPlayingMovies());
    }
}
