package com.moviesocial.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.moviesocial.model.Report;
import com.moviesocial.model.Report.ReportStatus;
import com.moviesocial.model.User;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    
    // 상태별 신고 목록 조회
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
    
    // 상태별 신고 목록 조회 (페이징 없음)
    List<Report> findByStatus(ReportStatus status);
    
    // 특정 사용자가 제출한 신고 목록 조회
    Page<Report> findByReporter(User reporter, Pageable pageable);
    
    // 특정 사용자에 대한 신고 목록 조회
    Page<Report> findByTargetUser(User targetUser, Pageable pageable);
    
    // 신고 타입별 조회
    Page<Report> findByReportType(Report.ReportType reportType, Pageable pageable);
    
    // 특정 타겟 ID에 대한 신고 (예: 특정 게시글에 대한 신고들)
    List<Report> findByTargetIdAndReportType(Long targetId, Report.ReportType reportType);
    
    // 특정 상태가 아닌 신고 목록 조회 (예: REJECTED가 아닌 모든 신고)
    List<Report> findByStatusNot(ReportStatus status);
} 