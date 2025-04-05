package com.moviesocial.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자가 스크랩한 콘텐츠(영화/TV) 정보를 저장하는 엔티티
 */
@Data
@Entity
@Table(name = "content_scraps", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "content_id", "media_type"})
})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentScrap {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "content_id", nullable = false)
    private Long contentId;
    
    @Column(name = "media_type", nullable = false)
    private String mediaType; // "movie" 또는 "tv"
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "poster_path")
    private String posterPath;
    
    @Column(name = "vote_average")
    private Double voteAverage;
    
    @Column(name = "vote_count")
    private Integer voteCount;
    
    @Column(name = "release_date")
    private String releaseDate;
    
    @Column(name = "backdrop_path")
    private String backdropPath;
    
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
} 