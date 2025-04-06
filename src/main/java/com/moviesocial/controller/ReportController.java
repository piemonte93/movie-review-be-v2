package com.moviesocial.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.moviesocial.model.Report;
import com.moviesocial.model.Report.ReportStatus;
import com.moviesocial.model.Report.ReportType;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.ReportRequest;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.PagedResponse;
import com.moviesocial.payload.response.ReportResponse;
import com.moviesocial.service.ReportService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 생성 API
     */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ReportRequest reportRequest) {
        
        // 유저명으로 유저 정보 확인
        Long reporterId = null;
        try {
            // UserRepository를 통해 직접 처리
            // Report 서비스에 유저명 전달로 변경 가능
            reporterId = reportService.getUserIdByUsername(userDetails.getUsername());
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        
        Report report = reportService.createReport(
                reporterId,
                reportRequest.getTargetUserId(),
                reportRequest.getTargetId(),
                reportRequest.getReportType(),
                reportRequest.getContent()
        );
        
        log.info("신고가 등록되었습니다. 신고 ID: {}", report.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(convertToReportResponse(report));
    }
    
    /**
     * 전체 신고 목록 조회 (관리자용)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ReportResponse>> getAllReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));
        Page<Report> reports = reportService.getAllReports(pageable);
        
        return ResponseEntity.ok(createPagedResponse(reports));
    }
    
    /**
     * 특정 상태의 신고 목록 조회 (관리자용)
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ReportResponse>> getReportsByStatus(
            @PathVariable ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportService.getReportsByStatus(status, pageable);
        
        return ResponseEntity.ok(createPagedResponse(reports));
    }
    
    /**
     * 특정 유형의 신고 목록 조회 (관리자용)
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ReportResponse>> getReportsByType(
            @PathVariable ReportType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportService.getReportsByType(type, pageable);
        
        return ResponseEntity.ok(createPagedResponse(reports));
    }
    
    /**
     * 신고자별 신고 목록 조회
     */
    @GetMapping("/reporter/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#userId)")
    public ResponseEntity<PagedResponse<ReportResponse>> getReportsByReporter(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportService.getReportsByReporter(userId, pageable);
        
        return ResponseEntity.ok(createPagedResponse(reports));
    }
    
    /**
     * 신고 대상자별 신고 목록 조회 (관리자용)
     */
    @GetMapping("/target-user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<ReportResponse>> getReportsByTargetUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> reports = reportService.getReportsByTargetUser(userId, pageable);
        
        return ResponseEntity.ok(createPagedResponse(reports));
    }
    
    /**
     * 신고 상태 업데이트 (관리자용)
     */
    @PutMapping("/{reportId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReportResponse> updateReportStatus(
            @PathVariable Long reportId,
            @RequestParam ReportStatus status) {
        
        Report updatedReport = reportService.updateReportStatus(reportId, status);
        log.info("신고 ID: {}의 상태가 {}로 업데이트되었습니다.", reportId, status);
        
        return ResponseEntity.ok(convertToReportResponse(updatedReport));
    }
    
    /**
     * 신고 상세 조회
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN') or @reportSecurity.canViewReport(#reportId)")
    public ResponseEntity<ReportResponse> getReportDetails(@PathVariable Long reportId) {
        Report report = reportService.getReportById(reportId);
        return ResponseEntity.ok(convertToReportResponse(report));
    }
    
    /**
     * 신고 엔티티를 ReportResponse로 변환
     */
    private ReportResponse convertToReportResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .content(report.getContent())
                .reportType(report.getReportType())
                .targetId(report.getTargetId())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .reporter(new ReportResponse.UserInfo(
                        report.getReporter().getId(),
                        report.getReporter().getUsername(),
                        report.getReporter().getProfileImageUrl()))
                .targetUser(new ReportResponse.UserInfo(
                        report.getTargetUser().getId(),
                        report.getTargetUser().getUsername(),
                        report.getTargetUser().getProfileImageUrl()))
                .build();
    }
    
    /**
     * Page<Report>를 PagedResponse<ReportResponse>로 변환
     */
    private PagedResponse<ReportResponse> createPagedResponse(Page<Report> reportPage) {
        List<ReportResponse> reportResponses = reportPage.getContent().stream()
                .map(this::convertToReportResponse)
                .collect(Collectors.toList());
        
        return new PagedResponse<>(
                reportResponses,
                reportPage.getNumber(),
                reportPage.getSize(),
                reportPage.getTotalElements(),
                reportPage.getTotalPages(),
                reportPage.isLast()
        );
    }
} 