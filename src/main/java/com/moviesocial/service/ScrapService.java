package com.moviesocial.service;

import com.moviesocial.model.ContentScrap;
import com.moviesocial.model.User;
import com.moviesocial.repository.ContentScrapRepository;
import com.moviesocial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.moviesocial.model.tmdb.ContentDetail;

/**
 * 콘텐츠 스크랩 관련 비즈니스 로직을 처리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapService {
    private final ContentScrapRepository scrapRepository;
    private final UserRepository userRepository;
    private final TmdbApiService tmdbApiService;
    
    /**
     * 사용자의 스크랩 목록 조회
     * @param username 사용자명
     * @return 스크랩 목록
     */
    public List<Map<String, Object>> getUserScraps(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        List<ContentScrap> scraps = scrapRepository.findByUserOrderByCreatedAtDesc(user);
        log.debug("사용자 {} 스크랩 {} 개 조회", username, scraps.size());
        
        return scraps.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    /**
     * 콘텐츠 스크랩 여부 확인
     * @param username 사용자명
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @return 스크랩 여부
     */
    public boolean isContentScraped(String username, Long contentId, String mediaType) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        boolean isScraped = scrapRepository.existsByUserAndContentIdAndMediaType(user, contentId, mediaType);
        log.debug("사용자 {} 콘텐츠 {} 타입 {} 스크랩 여부: {}", username, contentId, mediaType, isScraped);
        
        return isScraped;
    }
    
    /**
     * 콘텐츠 스크랩 토글 (스크랩 추가/삭제)
     * @param username 사용자명
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @return 토글 후 스크랩 상태 (true: 스크랩됨, false: 스크랩 취소됨)
     */
    @Transactional
    public boolean toggleContentScrap(String username, Long contentId, String mediaType) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));
        
        Optional<ContentScrap> existingScrap = 
            scrapRepository.findByUserAndContentIdAndMediaType(user, contentId, mediaType);
        
        if (existingScrap.isPresent()) {
            // 스크랩 삭제
            scrapRepository.delete(existingScrap.get());
            log.debug("사용자 {} 콘텐츠 {} 타입 {} 스크랩 삭제", username, contentId, mediaType);
            return false; // 스크랩 취소
        } else {
            // 스크랩 추가
            ContentScrap newScrap = createNewScrap(user, contentId, mediaType);
            scrapRepository.save(newScrap);
            log.debug("사용자 {} 콘텐츠 {} 타입 {} 스크랩 추가", username, contentId, mediaType);
            return true; // 스크랩 추가
        }
    }
    
    /**
     * TMDB API로부터 콘텐츠 정보를 가져와 새 스크랩 객체 생성
     * @param user 사용자 객체
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @return 새 스크랩 객체
     */
    private ContentScrap createNewScrap(User user, Long contentId, String mediaType) {
        ContentDetail contentDetails;
        
        try {
            if ("movie".equals(mediaType)) {
                contentDetails = tmdbApiService.getMovieDetails(contentId);
            } else {
                contentDetails = tmdbApiService.getTvDetails(contentId);
            }
            
            ContentScrap scrap = new ContentScrap();
            scrap.setUser(user);
            scrap.setContentId(contentId);
            scrap.setMediaType(mediaType);
            
            // TMDB에서 받아온 정보 저장
            scrap.setTitle("movie".equals(mediaType) ? contentDetails.getTitle() : contentDetails.getName());
            scrap.setPosterPath(contentDetails.getPosterPath());
            scrap.setVoteAverage(contentDetails.getVoteAverage());
            scrap.setVoteCount(contentDetails.getVoteCount());
            scrap.setReleaseDate("movie".equals(mediaType) ? contentDetails.getReleaseDate() : contentDetails.getFirstAirDate());
            scrap.setBackdropPath(contentDetails.getBackdropPath());
            scrap.setOverview(contentDetails.getOverview());
            scrap.setCreatedAt(LocalDateTime.now());
            
            return scrap;
        } catch (Exception e) {
            log.error("콘텐츠 정보 조회 실패 contentId={}, mediaType={}", contentId, mediaType, e);
            
            // 에러 발생 시 기본 정보만 포함된 스크랩 생성
            ContentScrap scrap = new ContentScrap();
            scrap.setUser(user);
            scrap.setContentId(contentId);
            scrap.setMediaType(mediaType);
            scrap.setCreatedAt(LocalDateTime.now());
            
            return scrap;
        }
    }
    
    /**
     * 스크랩 엔티티를 DTO로 변환
     * @param scrap 스크랩 엔티티
     * @return DTO
     */
    private Map<String, Object> convertToDto(ContentScrap scrap) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", scrap.getContentId());
        dto.put("media_type", scrap.getMediaType());
        
        // TV 프로그램인 경우 name 필드도 추가
        if ("tv".equals(scrap.getMediaType())) {
            dto.put("name", scrap.getTitle());
            dto.put("title", scrap.getTitle()); // ContentCard 호환성을 위해 title도 설정
            dto.put("first_air_date", scrap.getReleaseDate());
        } else {
            dto.put("title", scrap.getTitle());
            dto.put("release_date", scrap.getReleaseDate());
        }
        
        dto.put("poster_path", scrap.getPosterPath());
        dto.put("vote_average", scrap.getVoteAverage());
        dto.put("vote_count", scrap.getVoteCount());
        dto.put("backdrop_path", scrap.getBackdropPath());
        dto.put("overview", scrap.getOverview());
        
        return dto;
    }
    
    // 유틸리티 메소드: 맵에서 안전하게 String 값 가져오기
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? String.valueOf(value) : defaultValue;
    }
    
    // 유틸리티 메소드: 맵에서 안전하게 Double 값 가져오기
    private Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        
        try {
            if (value instanceof Double) return (Double) value;
            if (value instanceof Integer) return ((Integer) value).doubleValue();
            if (value instanceof String) return Double.parseDouble((String) value);
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    // 유틸리티 메소드: 맵에서 안전하게 Integer 값 가져오기
    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        
        try {
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Double) return ((Double) value).intValue();
            if (value instanceof String) return Integer.parseInt((String) value);
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * 팔로잉하는 사용자들의 스크랩 목록 조회
     * @param followingUserIds 팔로잉하는 사용자 ID 목록
     * @return 스크랩 목록
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowingScraps(List<Long> followingUserIds) {
        if (followingUserIds == null || followingUserIds.isEmpty()) {
            log.debug("팔로잉하는 사용자가 없습니다.");
            return List.of();
        }
        
        log.debug("팔로잉 사용자 스크랩 조회: userIds={}", followingUserIds);
        
        // 1. 팔로잉하는 사용자들의 스크랩 조회
        List<ContentScrap> scraps = scrapRepository.findByUserIdInOrderByCreatedAtDesc(followingUserIds);
        log.debug("조회된 스크랩 수: {}", scraps.size());
        
        // 2. DTO로 변환하여 반환
        return scraps.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
} 