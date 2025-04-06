package com.moviesocial.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Size(max = 1000)
    private String content;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportType reportType;
    
    @NotNull
    private Long targetId;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private ReportStatus status;
    
    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    
    @ManyToOne
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }
    
    public enum ReportType {
        POST, COMMENT, REVIEW
    }
    
    public enum ReportStatus {
        PENDING, PROCESSED, REJECTED
    }
} 