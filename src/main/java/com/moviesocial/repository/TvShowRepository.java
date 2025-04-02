package com.moviesocial.repository;

import com.moviesocial.model.TvShow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TvShowRepository extends JpaRepository<TvShow, Long> {
    @Query("SELECT t FROM TvShow t WHERE " +
           "(:genres IS NULL OR EXISTS (SELECT 1 FROM t.genreIds g WHERE g IN :genres)) AND " +
           "(:year IS NULL OR YEAR(t.firstAirDate) = :year) AND " +
           "(:query IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:voteMin IS NULL OR t.voteAverage >= :voteMin) AND " +
           "(:isKorean IS NULL OR t.isKorean = :isKorean) AND " +
           "(:isForeign IS NULL OR t.isForeign = :isForeign) AND " +
           "(:network IS NULL OR :network MEMBER OF t.networks)")
    Page<TvShow> findByFilters(
            @Param("genres") List<Integer> genres,
            @Param("year") Integer year,
            @Param("query") String query,
            @Param("voteMin") Double voteMin,
            @Param("isKorean") Boolean isKorean,
            @Param("isForeign") Boolean isForeign,
            @Param("network") String network,
            Pageable pageable
    );
} 