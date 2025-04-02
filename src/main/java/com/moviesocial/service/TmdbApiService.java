package com.moviesocial.service;

import com.moviesocial.model.tmdb.ContentDetail;
import com.moviesocial.model.tmdb.ContentResponse;
import com.moviesocial.model.tmdb.ReviewResponse;
import com.moviesocial.model.tmdb.VideoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("TMDB API Key: {}", apiKey != null ? "설정됨" : "설정되지 않음");
        log.info("TMDB Base URL: {}", baseUrl);
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

        ContentDetail contentDetail = restTemplate.getForObject(url, ContentDetail.class);
        
        // 출연진과 제작진 정보 가져오기
        if (contentDetail != null) {
            ContentDetail creditsInfo = getMovieCredits(movieId);
            if (creditsInfo != null) {
                contentDetail.setCast(creditsInfo.getCast());
                contentDetail.setCrew(creditsInfo.getCrew());
            }
        }
        
        return contentDetail;
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

        ContentDetail contentDetail = restTemplate.getForObject(url, ContentDetail.class);
        
        // 출연진과 제작진 정보 가져오기
        if (contentDetail != null) {
            ContentDetail creditsInfo = getTvCredits(tvId);
            if (creditsInfo != null) {
                contentDetail.setCast(creditsInfo.getCast());
                contentDetail.setCrew(creditsInfo.getCrew());
            }
        }
        
        return contentDetail;
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

    /**
     * 콘텐츠 검색 메서드
     * @param query 검색어
     * @param page 페이지 번호
     * @return 검색 결과
     */
    public ContentResponse searchContents(String query, int page) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/search/multi")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("query", query)
                .queryParam("page", page)
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentResponse.class);
    }

    /**
     * 필터링된 검색 메서드 - TMDB의 검색 API는 필터링 기능을 제공하지 않으므로
     * 영화만 검색하도록 수정된 버전
     * @param query 검색어
     * @param page 페이지 번호
     * @param genres 장르 필터
     * @param year 연도 필터
     * @param voteAvgMin 최소 평점
     * @param isKorean 한국 영화 필터
     * @param isForeign 외국 영화 필터
     * @return 검색 결과
     */
    public ContentResponse searchMoviesWithFilters(String query, int page, String genres,
                                                   Integer year, Double voteAvgMin,
                                                   Boolean isKorean, Boolean isForeign) {
        // 기본 검색 URL 구성
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/search/movie") // multi 대신 movie만 검색
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("query", query)
                .queryParam("page", 1) // 더 많은 결과를 얻기 위해 첫 페이지는 항상 1로 설정
                .queryParam("include_adult", false);

        // 필터 적용 (TMDB API에서 지원하는 검색 필터는 제한적임)
        // 연도 필터 적용
        if (year != null) {
            builder.queryParam("primary_release_year", year);
        }

        // 국내/외국 영화 필터는 검색 결과 후처리가 필요할 수 있음 (TMDB API 제한)
        // 그러나 일부 제한적인 필터링은 가능함
        if (Boolean.TRUE.equals(isKorean)) {
            builder.queryParam("with_original_language", "ko");
        }

        String url = builder.build().toUriString();
        ContentResponse response = restTemplate.getForObject(url, ContentResponse.class);

        // 결과가 없으면 빈 응답 반환
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return response;
        }

        // 장르 필터링과 추가 필터링 적용 (TMDB API에서 지원하지 않는 필터는 백엔드에서 처리)
        List<ContentResponse.ContentItem> filteredResults = new ArrayList<>(response.getResults());

        // 외국 영화 필터 처리 (API에서 지원하지 않으므로 후처리)
        if (Boolean.TRUE.equals(isForeign)) {
            filteredResults = filteredResults.stream()
                    .filter(item -> !"ko".equals(item.getOriginal_language()))
                    .collect(Collectors.toList());
        }

        // 장르 필터링 (검색 API는 장르 필터를 지원하지 않으므로 후처리 필요)
        if (genres != null && !genres.isEmpty()) {
            List<Integer> genreList = Arrays.stream(genres.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            filteredResults = filteredResults.stream()
                    .filter(item -> {
                        // 영화의 장르 ID 목록이 없으면 필터링하지 않음
                        if (item.getGenre_ids() == null || item.getGenre_ids().isEmpty()) {
                            return false;
                        }
                        // 지정한 장르 중 하나라도 영화에 포함되어 있으면 통과
                        return item.getGenre_ids().stream().anyMatch(genreList::contains);
                    })
                    .collect(Collectors.toList());
        }

        // 검색어로 여러 페이지의 결과를 가져와 필터링 후 페이징 처리
        if (filteredResults.size() < 20 && response.getTotal_pages() > 1) {
            // 최대 3페이지까지만 추가로 가져옴 (성능 고려)
            int maxPages = Math.min(3, response.getTotal_pages());

            for (int i = 2; i <= maxPages; i++) {
                String nextPageUrl = UriComponentsBuilder
                        .fromHttpUrl(baseUrl + "/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "ko-KR")
                        .queryParam("query", query)
                        .queryParam("page", i)
                        .queryParam("include_adult", false)
                        .build()
                        .toUriString();

                ContentResponse nextPageResponse = restTemplate.getForObject(nextPageUrl, ContentResponse.class);

                if (nextPageResponse != null && nextPageResponse.getResults() != null &&
                        !nextPageResponse.getResults().isEmpty()) {

                    List<ContentResponse.ContentItem> nextPageFiltered = new ArrayList<>(nextPageResponse.getResults());

                    // 외국 영화 필터 적용
                    if (Boolean.TRUE.equals(isForeign)) {
                        nextPageFiltered = nextPageFiltered.stream()
                                .filter(item -> !"ko".equals(item.getOriginal_language()))
                                .collect(Collectors.toList());
                    }

                    // 장르 필터 적용
                    if (genres != null && !genres.isEmpty()) {
                        List<Integer> genreList = Arrays.stream(genres.split(","))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        nextPageFiltered = nextPageFiltered.stream()
                                .filter(item -> {
                                    if (item.getGenre_ids() == null || item.getGenre_ids().isEmpty()) {
                                        return false;
                                    }
                                    return item.getGenre_ids().stream().anyMatch(genreList::contains);
                                })
                                .collect(Collectors.toList());
                    }

                    // 추가 결과 합치기
                    filteredResults.addAll(nextPageFiltered);
                }
            }
        }

        // 필터링된 결과에 대해 페이징 처리
        int startIndex = (page - 1) * 20;
        List<ContentResponse.ContentItem> pagedResults;

        if (startIndex < filteredResults.size()) {
            int endIndex = Math.min(startIndex + 20, filteredResults.size());
            pagedResults = filteredResults.subList(startIndex, endIndex);
        } else {
            pagedResults = Collections.emptyList();
        }

        // 새 응답 생성
        ContentResponse filteredResponse = new ContentResponse();
        filteredResponse.setPage(page);
        filteredResponse.setResults(pagedResults);
        filteredResponse.setTotal_pages((int) Math.ceil(filteredResults.size() / 20.0));
        filteredResponse.setTotal_results(filteredResults.size());

        return filteredResponse;
    }

    /**
     * 영화 필터링 및 발견 메서드
     * @param genre 장르 ID
     * @param year 년도
     * @param sortBy 정렬 기준
     * @param page 페이지 번호
     * @return 필터링된 영화 목록
     */
    public ContentResponse discoverMovies(Integer genre, Integer year, String sortBy, int page) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/discover/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", sortBy)
                .queryParam("page", page)
                .queryParam("region", "KR");

        // 선택적 파라미터 추가
        if (genre != null) {
            builder.queryParam("with_genres", genre);
        }

        if (year != null) {
            builder.queryParam("primary_release_year", year);
        }

        String url = builder.build().toUriString();
        return restTemplate.getForObject(url, ContentResponse.class);
    }

    /**
     * 영화 필터링 및 발견 메서드 - 다중 장르 및 검색어 지원
     * @param genres 장르 ID 목록 (콤마로 구분된 문자열)
     * @param year 년도
     * @param sortBy 정렬 기준
     * @param page 페이지 번호
     * @param query 검색어
     * @return 필터링된 영화 목록
     */
    public ContentResponse discoverMovies(String genres, Integer year, String sortBy, int page, String query) {
        // 검색어가 있으면 검색 API를 사용
        if (query != null && !query.isEmpty()) {
            return searchContents(query, page);
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/discover/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", sortBy)
                .queryParam("page", page)
                .queryParam("region", "KR");

        // 선택적 파라미터 추가
        if (genres != null && !genres.isEmpty()) {
            builder.queryParam("with_genres", genres);
        }

        if (year != null) {
            builder.queryParam("primary_release_year", year);
        }

        String url = builder.build().toUriString();
        return restTemplate.getForObject(url, ContentResponse.class);
    }

    /**
     * 영화 필터링 및 발견 메서드 - 다중 장르, 검색어, 평점 범위 지원
     * @param genres 장르 ID 목록 (콤마로 구분된 문자열)
     * @param year 년도
     * @param sortBy 정렬 기준
     * @param page 페이지 번호
     * @param query 검색어
     * @param voteAvgMin 최소 평점 (0-10)
     * @param voteAvgMax 최대 평점 (0-10)
     * @return 필터링된 영화 목록
     */
    public ContentResponse discoverMovies(String genres, Integer year, String sortBy, int page, String query,
                                          Double voteAvgMin, Double voteAvgMax) {
        // 검색어가 있으면 검색 API를 사용
        if (query != null && !query.isEmpty()) {
            return searchContents(query, page);
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/discover/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", sortBy)
                .queryParam("page", page)
                .queryParam("region", "KR");

        // 선택적 파라미터 추가
        if (genres != null && !genres.isEmpty()) {
            builder.queryParam("with_genres", genres);
        }

        if (year != null) {
            builder.queryParam("primary_release_year", year);
        }

        // 평점 필터 추가
        if (voteAvgMin != null) {
            builder.queryParam("vote_average.gte", voteAvgMin);
        }

        if (voteAvgMax != null) {
            builder.queryParam("vote_average.lte", voteAvgMax);
        }

        String url = builder.build().toUriString();
        return restTemplate.getForObject(url, ContentResponse.class);
    }

    /**
     * 영화 필터링 및 발견 메서드 - 다중 장르, 검색어, 평점 범위 및 국가 필터 지원
     * @param genres 장르 ID 목록 (콤마로 구분된 문자열)
     * @param year 년도
     * @param sortBy 정렬 기준
     * @param page 페이지 번호
     * @param query 검색어
     * @param voteAvgMin 최소 평점 (0-10)
     * @param voteAvgMax 최대 평점 (0-10)
     * @param isKorean 한국 영화 필터
     * @param isForeign 해외 영화 필터
     * @return 필터링된 영화 목록
     */
    public ContentResponse discoverMovies(String genres, Integer year, String sortBy, int page, String query,
                                          Double voteAvgMin, Double voteAvgMax, Boolean isKorean, Boolean isForeign) {
        // 검색어가 있으면 필터링된 검색 API를, 없으면 일반 discover API 사용
        if (query != null && !query.isEmpty()) {
            return searchMoviesWithFilters(query, page, genres, year, voteAvgMin, isKorean, isForeign);
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/discover/movie")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", sortBy)
                .queryParam("page", page);

        // 국내영화 필터링 - 한국영화만 보기
        if (Boolean.TRUE.equals(isKorean)) {
            builder.queryParam("with_original_language", "ko");
        }

        // 해외영화 필터링 - 한국영화 제외하기
        if (Boolean.TRUE.equals(isForeign)) {
            builder.queryParam("without_original_language", "ko");
        }

        // 지역 설정은 항상 유지 (인기도 계산용)
        builder.queryParam("region", "KR");

        // 선택적 파라미터 추가
        if (genres != null && !genres.isEmpty()) {
            builder.queryParam("with_genres", genres);
        }

        if (year != null) {
            builder.queryParam("primary_release_year", year);
        }

        // 평점 필터 추가
        if (voteAvgMin != null) {
            builder.queryParam("vote_average.gte", voteAvgMin);
        }

        if (voteAvgMax != null) {
            builder.queryParam("vote_average.lte", voteAvgMax);
        }

        String url = builder.build().toUriString();
        return restTemplate.getForObject(url, ContentResponse.class);
    }

    // 영화 출연진/제작진 정보를 가져오는 메서드
    public ContentDetail getMovieCredits(Long movieId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/movie/" + movieId + "/credits")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentDetail.class);
    }

    // TV 프로그램 출연진/제작진 정보를 가져오는 메서드
    public ContentDetail getTvCredits(Long tvId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/tv/" + tvId + "/credits")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ContentDetail.class);
    }

    /**
     * TV 프로그램 필터링 및 발견 메서드
     * @param genres 장르 ID 목록 (콤마로 구분된 문자열)
     * @param year 년도
     * @param sortBy 정렬 기준
     * @param page 페이지 번호
     * @param query 검색어
     * @param voteAvgMin 최소 평점
     * @param voteAvgMax 최대 평점
     * @param isKorean 한국 TV 프로그램 필터
     * @param isForeign 해외 TV 프로그램 필터
     * @param network 방송사
     * @return 필터링된 TV 프로그램 목록
     */
    public ContentResponse discoverTvShows(String genres, Integer year, String sortBy, int page, String query,
                                         Double voteAvgMin, Double voteAvgMax, Boolean isKorean, Boolean isForeign,
                                         String network) {
        // 검색어가 있으면 필터링된 검색 API를, 없으면 일반 discover API 사용
        if (query != null && !query.isEmpty()) {
            return searchTvShowsWithFilters(query, page, genres, year, voteAvgMin, isKorean, isForeign, network);
        }

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/discover/tv")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("sort_by", sortBy)
                .queryParam("page", page);

        // 국내TV 프로그램 필터링 - 한국TV 프로그램만 보기
        if (Boolean.TRUE.equals(isKorean)) {
            builder.queryParam("with_original_language", "ko");
        }

        // 해외TV 프로그램 필터링 - 한국TV 프로그램 제외하기
        if (Boolean.TRUE.equals(isForeign)) {
            builder.queryParam("without_original_language", "ko");
        }

        // 선택적 파라미터 추가
        if (genres != null && !genres.isEmpty()) {
            builder.queryParam("with_genres", genres);
        }

        if (year != null) {
            builder.queryParam("first_air_date_year", year);
        }

        // 평점 필터 추가
        if (voteAvgMin != null) {
            builder.queryParam("vote_average.gte", voteAvgMin);
        }

        if (voteAvgMax != null) {
            builder.queryParam("vote_average.lte", voteAvgMax);
        }

        // 방송사 필터 추가
        if (network != null && !network.isEmpty()) {
            builder.queryParam("with_networks", network);
        }

        String url = builder.build().toUriString();
        return restTemplate.getForObject(url, ContentResponse.class);
    }

    /**
     * TV 프로그램 필터링된 검색 메서드
     * @param query 검색어
     * @param page 페이지 번호
     * @param genres 장르 ID 목록
     * @param year 년도
     * @param voteAvgMin 최소 평점
     * @param isKorean 한국 TV 프로그램 필터
     * @param isForeign 해외 TV 프로그램 필터
     * @param network 방송사
     * @return 필터링된 검색 결과
     */
    public ContentResponse searchTvShowsWithFilters(String query, int page, String genres,
                                                  Integer year, Double voteAvgMin,
                                                  Boolean isKorean, Boolean isForeign,
                                                  String network) {
        // 기본 검색 URL 구성
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl + "/search/tv")
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("query", query)
                .queryParam("page", 1)
                .queryParam("include_adult", false);

        // 필터 적용
        if (year != null) {
            builder.queryParam("first_air_date_year", year);
        }

        if (Boolean.TRUE.equals(isKorean)) {
            builder.queryParam("with_original_language", "ko");
        }

        String url = builder.build().toUriString();
        ContentResponse response = restTemplate.getForObject(url, ContentResponse.class);

        // 결과가 없으면 빈 응답 반환
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return response;
        }

        // 장르 필터링과 추가 필터링 적용
        List<ContentResponse.ContentItem> filteredResults = new ArrayList<>(response.getResults());

        // 외국 TV 프로그램 필터 처리
        if (Boolean.TRUE.equals(isForeign)) {
            filteredResults = filteredResults.stream()
                    .filter(item -> !"ko".equals(item.getOriginal_language()))
                    .collect(Collectors.toList());
        }

        // 장르 필터링
        if (genres != null && !genres.isEmpty()) {
            List<Integer> genreList = Arrays.stream(genres.split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            filteredResults = filteredResults.stream()
                    .filter(item -> {
                        if (item.getGenre_ids() == null || item.getGenre_ids().isEmpty()) {
                            return false;
                        }
                        return item.getGenre_ids().stream().anyMatch(genreList::contains);
                    })
                    .collect(Collectors.toList());
        }

        // 검색어로 여러 페이지의 결과를 가져와 필터링 후 페이징 처리
        if (filteredResults.size() < 20 && response.getTotal_pages() > 1) {
            int maxPages = Math.min(3, response.getTotal_pages());

            for (int i = 2; i <= maxPages; i++) {
                String nextPageUrl = UriComponentsBuilder
                        .fromHttpUrl(baseUrl + "/search/tv")
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "ko-KR")
                        .queryParam("query", query)
                        .queryParam("page", i)
                        .queryParam("include_adult", false)
                        .build()
                        .toUriString();

                ContentResponse nextPageResponse = restTemplate.getForObject(nextPageUrl, ContentResponse.class);

                if (nextPageResponse != null && nextPageResponse.getResults() != null &&
                        !nextPageResponse.getResults().isEmpty()) {

                    List<ContentResponse.ContentItem> nextPageFiltered = new ArrayList<>(nextPageResponse.getResults());

                    // 외국 TV 프로그램 필터 적용
                    if (Boolean.TRUE.equals(isForeign)) {
                        nextPageFiltered = nextPageFiltered.stream()
                                .filter(item -> !"ko".equals(item.getOriginal_language()))
                                .collect(Collectors.toList());
                    }

                    // 장르 필터 적용
                    if (genres != null && !genres.isEmpty()) {
                        List<Integer> genreList = Arrays.stream(genres.split(","))
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());

                        nextPageFiltered = nextPageFiltered.stream()
                                .filter(item -> {
                                    if (item.getGenre_ids() == null || item.getGenre_ids().isEmpty()) {
                                        return false;
                                    }
                                    return item.getGenre_ids().stream().anyMatch(genreList::contains);
                                })
                                .collect(Collectors.toList());
                    }

                    filteredResults.addAll(nextPageFiltered);
                }
            }
        }

        // 필터링된 결과에 대해 페이징 처리
        int startIndex = (page - 1) * 20;
        List<ContentResponse.ContentItem> pagedResults;

        if (startIndex < filteredResults.size()) {
            int endIndex = Math.min(startIndex + 20, filteredResults.size());
            pagedResults = filteredResults.subList(startIndex, endIndex);
        } else {
            pagedResults = Collections.emptyList();
        }

        // 새 응답 생성
        ContentResponse filteredResponse = new ContentResponse();
        filteredResponse.setPage(page);
        filteredResponse.setResults(pagedResults);
        filteredResponse.setTotal_pages((int) Math.ceil(filteredResults.size() / 20.0));
        filteredResponse.setTotal_results(filteredResults.size());

        return filteredResponse;
    }
}