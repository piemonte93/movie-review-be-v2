package com.moviesocial.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tv_shows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TvShow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String overview;

    private String posterPath;

    private String backdropPath;

    private LocalDate firstAirDate;

    private LocalDate lastAirDate;

    private Double voteAverage;

    private Integer voteCount;

    private String status;

    @ElementCollection
    @CollectionTable(name = "tv_show_genres")
    private Set<Integer> genreIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "tv_show_networks")
    private Set<String> networks = new HashSet<>();

    @Column(nullable = false)
    private String originalLanguage;

    @Builder.Default
    private boolean isKorean = false;

    @Builder.Default
    private boolean isForeign = false;
} 