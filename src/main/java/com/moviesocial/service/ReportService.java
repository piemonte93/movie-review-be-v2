package com.moviesocial.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviesocial.exception.ResourceNotFoundException;
import com.moviesocial.model.Report;
import com.moviesocial.model.Report.ReportStatus;
import com.moviesocial.model.Report.ReportType;
import com.moviesocial.model.User;
import com.moviesocial.repository.ReportRepository;
import com.moviesocial.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {
    
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    
    /**
     * 사용자명으로 사용자 ID 조회
     */
    public Long getUserIdByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return user.getId();
    }
    
    /**
     * 새로운 신고 생성
     */
    @Transactional
    public Report createReport(Long reporterId, Long targetUserId, Long targetId, ReportType reportType, String content) {
        // 신고자 정보 조회
        User reporter = userRepository.findById(reporterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", reporterId));
        
        // 신고 대상 사용자 정보 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
        
        Report report = Report.builder()
                .reporter(reporter)
                .targetUser(targetUser)
                .targetId(targetId)
                .reportType(reportType)
                .content(content)
                .status(ReportStatus.PENDING)
                .build();
        
        log.info("Creating new report: reporter={}, targetUser={}, targetId={}, type={}", 
                reporter.getUsername(), targetUser.getUsername(), targetId, reportType);
        
        return reportRepository.save(report);
    }
    
    /**
     * 신고 상태 업데이트
     */
    @Transactional
    public Report updateReportStatus(Long reportId, ReportStatus status) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        
        report.setStatus(status);
        log.info("Updating report status: id={}, newStatus={}", reportId, status);
        
        return reportRepository.save(report);
    }
    
    /**
     * 전체 신고 목록 조회 (페이징)
     */
    public Page<Report> getAllReports(Pageable pageable) {
        return reportRepository.findAll(pageable);
    }
    
    /**
     * 특정 상태의 신고 목록 조회
     */
    public Page<Report> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable);
    }
    
    /**
     * 특정 유형의 신고 목록 조회
     */
    public Page<Report> getReportsByType(ReportType reportType, Pageable pageable) {
        return reportRepository.findByReportType(reportType, pageable);
    }
    
    /**
     * 특정 사용자의 신고 목록 조회
     */
    public Page<Report> getReportsByReporter(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return reportRepository.findByReporter(user, pageable);
    }
    
    /**
     * 특정 사용자에 대한 신고 목록 조회
     */
    public Page<Report> getReportsByTargetUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        return reportRepository.findByTargetUser(user, pageable);
    }
    
    /**
     * 특정 대상(게시글, 댓글, 리뷰)에 대한 신고 목록 조회
     */
    public List<Report> getReportsByTarget(Long targetId, ReportType reportType) {
        return reportRepository.findByTargetIdAndReportType(targetId, reportType);
    }
    
    /**
     * 신고 상세 조회
     */
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
    }
} 