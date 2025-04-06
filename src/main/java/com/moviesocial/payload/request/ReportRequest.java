package com.moviesocial.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.moviesocial.model.Report.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    @NotNull(message = "신고 대상 ID는 필수입니다.")
    private Long targetId;
    
    @NotNull(message = "신고 대상 사용자 ID는 필수입니다.")
    private Long targetUserId;
    
    @NotNull(message = "신고 유형은 필수입니다.")
    private ReportType reportType;
    
    @NotBlank(message = "신고 내용은 필수입니다.")
    @Size(min = 10, max = 1000, message = "신고 내용은 10자 이상 1000자 이하로 작성해주세요.")
    private String content;
} 