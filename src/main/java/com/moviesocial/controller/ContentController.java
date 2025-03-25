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

    /**
     * 검색 API 엔드포인트
     * @param query 검색어
     * @param page 페이지 번호
     * @return 검색 결과
     */
    @GetMapping("/search")
    public ResponseEntity<ContentResponse> searchContents(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return ResponseEntity.ok(tmdbApiService.searchContents(query, page));
    }

    /**
     * 영화 필터링 및 발견 API 엔드포인트 - 통합 버전 (단일 장르, 다중 장르, 검색어 지원)
     * @param genre 단일 장르 ID
     * @param genres 장르 ID 목록 (콤마로 구분된 문자열)
     * @param year 년도
     * @param sortBy 정렬 기준
     * @param page 페이지 번호
     * @param query 검색어
     * @param voteAvgMin 최소 평점
     * @param voteAvgMax 최대 평점
     * @param isKorean 한국 영화 필터
     * @param isForeign 외국 영화 필터
     * @return 필터링된 영화 목록
     */
    @GetMapping("/discover/movie")
    public ResponseEntity<ContentResponse> discoverMovies(
            @RequestParam(required = false) Integer genre,
            @RequestParam(required = false) String genres,
            @RequestParam(required = false) Integer year,
            @RequestParam(name = "sort_by", defaultValue = "popularity.desc") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Double voteAvgMin,
            @RequestParam(required = false) Double voteAvgMax,
            @RequestParam(required = false) Boolean isKorean,
            @RequestParam(required = false) Boolean isForeign) {

        // 단일 장르와 다중 장르 모두 제공된 경우 다중 장르 사용
        String effectiveGenres = genres;
        if (effectiveGenres == null && genre != null) {
            effectiveGenres = genre.toString();
        }

        return ResponseEntity.ok(tmdbApiService.discoverMovies(effectiveGenres, year, sortBy, page, query,
                voteAvgMin, voteAvgMax, isKorean, isForeign));
    }
}