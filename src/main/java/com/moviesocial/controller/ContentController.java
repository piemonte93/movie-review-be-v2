package com.moviesocial.controller;

import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.model.tmdb.ContentResponse;
import com.moviesocial.model.tmdb.ReviewResponse;
import com.moviesocial.model.tmdb.VideoResponse;
import com.moviesocial.service.TmdbApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contents")
public class ContentController {

    private final TmdbApiService tmdbApiService;

    @Autowired
    public ContentController(TmdbApiService tmdbApiService) {
        this.tmdbApiService = tmdbApiService;
    }

    @GetMapping("/trending")
    public ResponseEntity<ContentResponse> getTrendingMovies() {
        return ResponseEntity.ok(tmdbApiService.getTrendingMovies());
    }

    @GetMapping("/trending-all")
    public ResponseEntity<ContentResponse> getTrendingAll() {
        return ResponseEntity.ok(tmdbApiService.getTrendingAll());
    }

    @GetMapping("/top-rated")
    public ResponseEntity<ContentResponse> getTopRatedMovies() {
        return ResponseEntity.ok(tmdbApiService.getTopRatedMovies());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ContentResponse> getUpcomingMovies() {
        return ResponseEntity.ok(tmdbApiService.getUpcomingMovies());
    }

    @GetMapping("/now-playing")
    public ResponseEntity<ContentResponse> getNowPlayingMovies() {
        return ResponseEntity.ok(tmdbApiService.getNowPlayingMovies());
    }

    /**
     * 콘텐츠 상세 정보를 가져오는 API 엔드포인트
     * @param id 콘텐츠 ID
     * @return 콘텐츠 상세 정보
     */
    @GetMapping("/movie/{id}")
    public ResponseEntity<ContentDetail> getMovieDetails(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbApiService.getMovieDetails(id));
    }

    /**
     * 콘텐츠 비디오 목록을 가져오는 API 엔드포인트
     * @param id 콘텐츠 ID
     * @return 콘텐츠 비디오 목록
     */
    @GetMapping("/movie/{id}/videos")
    public ResponseEntity<VideoResponse> getMovieVideos(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbApiService.getMovieVideos(id));
    }

    /**
     * 콘텐츠 리뷰 목록을 가져오는 API 엔드포인트
     * @param id 콘텐츠 ID
     * @return 콘텐츠 리뷰 목록
     */
    @GetMapping("/movie/{id}/reviews")
    public ResponseEntity<ReviewResponse> getMovieReviews(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbApiService.getMovieReviews(id));
    }
    
    /**
     * TV 프로그램 상세 정보를 가져오는 API 엔드포인트
     * @param id TV 프로그램 ID
     * @return TV 프로그램 상세 정보
     */
    @GetMapping("/tv/{id}")
    public ResponseEntity<ContentDetail> getTvDetails(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbApiService.getTvDetails(id));
    }

    /**
     * TV 프로그램 비디오 목록을 가져오는 API 엔드포인트
     * @param id TV 프로그램 ID
     * @return TV 프로그램 비디오 목록
     */
    @GetMapping("/tv/{id}/videos")
    public ResponseEntity<VideoResponse> getTvVideos(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbApiService.getTvVideos(id));
    }

    /**
     * TV 프로그램 리뷰 목록을 가져오는 API 엔드포인트
     * @param id TV 프로그램 ID
     * @return TV 프로그램 리뷰 목록
     */
    @GetMapping("/tv/{id}/reviews")
    public ResponseEntity<ReviewResponse> getTvReviews(@PathVariable Long id) {
        return ResponseEntity.ok(tmdbApiService.getTvReviews(id));
    }
}