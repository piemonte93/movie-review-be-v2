package com.moviesocial.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.moviesocial.model.Report;
import com.moviesocial.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportSecurity {
    
    private final ReportService reportService;
    
    /**
     * 현재 로그인 사용자가 신고를 볼 수 있는지 확인
     */
    public boolean canViewReport(Long reportId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        try {
            Report report = reportService.getReportById(reportId);
            String username = authentication.getName();
            
            // 신고자만 자신의 신고를 볼 수 있음
            return report.getReporter().getUsername().equals(username);
        } catch (Exception e) {
            log.error("ReportSecurity.canViewReport 에러: {}", e.getMessage());
            return false;
        }
    }
} 