package com.moviesocial.controller;

import com.moviesocial.service.ScrapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 콘텐츠 스크랩 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/scraps")
@RequiredArgsConstructor
@Slf4j
public class UserScrapController {
    
    private final ScrapService scrapService;
    
    /**
     * 사용자의 스크랩 목록 조회
     * @param userDetails 인증된 사용자 정보
     * @return 스크랩 목록
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Map<String, Object>>> getUserScraps(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("getUserScraps 요청: username={}", userDetails.getUsername());
        List<Map<String, Object>> scraps = scrapService.getUserScraps(userDetails.getUsername());
        return ResponseEntity.ok(scraps);
    }
    
    /**
     * 콘텐츠 스크랩 상태 확인
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @param userDetails 인증된 사용자 정보
     * @return 스크랩 상태 (scraped: true/false)
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> checkScrapStatus(
            @RequestParam Long contentId,
            @RequestParam String mediaType,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("checkScrapStatus 요청: username={}, contentId={}, mediaType={}", 
                userDetails.getUsername(), contentId, mediaType);
        
        boolean isScraped = scrapService.isContentScraped(userDetails.getUsername(), contentId, mediaType);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("scraped", isScraped);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 콘텐츠 스크랩 토글 (추가/삭제)
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @param userDetails 인증된 사용자 정보
     * @return 토글 후 스크랩 상태 (scraped: true/false)
     */
    @PostMapping("/toggle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> toggleScrap(
            @RequestParam Long contentId,
            @RequestParam String mediaType,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("toggleScrap 요청: username={}, contentId={}, mediaType={}", 
                userDetails.getUsername(), contentId, mediaType);
        
        boolean isScraped = scrapService.toggleContentScrap(userDetails.getUsername(), contentId, mediaType);
        
        Map<String, Boolean> response = new HashMap<>();
        response.put("scraped", isScraped);
        
        return ResponseEntity.ok(response);
    }
} 