package com.moviesocial.payload.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String profileImageUrl;
    private String bio;
    private boolean socialLogin;
    private List<String> roles;
    private String status;
    private String blockReason;
    private LocalDateTime blockDate;
    private int reportedCount;

    public void setReportedCount(int reportedCount) {
        this.reportedCount = reportedCount;
    }
} 