package com.moviesocial.payload.response;

import java.time.LocalDateTime;

import com.moviesocial.model.Report.ReportStatus;
import com.moviesocial.model.Report.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    
    private Long id;
    private String content;
    private ReportType reportType;
    private Long targetId;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private UserInfo reporter;
    private UserInfo targetUser;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String profileImageUrl;
    }
} 