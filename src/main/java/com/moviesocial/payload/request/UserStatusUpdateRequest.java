package com.moviesocial.payload.request;

import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    private String status; // "ACTIVE" 또는 "BLOCKED"
    private String reason; // 차단 사유 (선택적)
} 