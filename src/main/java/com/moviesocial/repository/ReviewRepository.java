package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    /**
     * 사용자의 리뷰 목록을 페이징하여 가져옵니다.
     * @param user 사용자
     * @param pageable 페이징 정보
     * @return 사용자 리뷰 목록
     */
    Page<Review> findByUser(User user, Pageable pageable);
    
    /**
     * 영화 ID로 리뷰 목록을 페이징하여 가져옵니다.
     * @param movieId 영화 ID
     * @param pageable 페이징 정보
     * @return 영화 리뷰 목록
     */
    Page<Review> findByMovieId(Long movieId, Pageable pageable);
    
    /**
     * 사용자와 영화 ID로 리뷰를 찾습니다.
     * @param user 사용자
     * @param movieId 영화 ID
     * @return 리뷰 (있는 경우)
     */
    Optional<Review> findByUserAndMovieId(User user, Long movieId);

    List<Review> findByUserIdAndMovieId(Long userId, Long movieId);
    
    /**
     * 콘텐츠 타입(movie/tv)으로 구분하여 모든 리뷰를 조회합니다.
     * @param contentType 콘텐츠 타입
     * @param pageable 페이징 정보
     * @return 리뷰 목록
     */
    Page<Review> findByContentType(String contentType, Pageable pageable);
    
    /**
     * 콘텐츠 타입과 콘텐츠 ID로 리뷰를 조회합니다.
     * @param contentType 콘텐츠 타입
     * @param movieId 콘텐츠 ID
     * @param pageable 페이징 정보
     * @return 리뷰 목록
     */
    Page<Review> findByContentTypeAndMovieId(String contentType, Long movieId, Pageable pageable);
    
    /**
     * 사용자와 콘텐츠 ID 및 콘텐츠 타입으로 리뷰를 찾습니다.
     * @param userId 사용자 ID
     * @param movieId 콘텐츠 ID
     * @param contentType 콘텐츠 타입
     * @return 리뷰 목록
     */
    List<Review> findByUserIdAndMovieIdAndContentType(Long userId, Long movieId, String contentType);
    
    /**
     * 사용자와 콘텐츠 타입으로 리뷰를 조회합니다.
     * @param user 사용자
     * @param contentType 콘텐츠 타입
     * @param pageable 페이징 정보
     * @return 리뷰 목록
     */
    Page<Review> findByUserAndContentType(User user, String contentType, Pageable pageable);

    Page<Review> findByUserId(Long userId, Pageable pageable);
    Page<Review> findByUserIdAndContentType(Long userId, String contentType, Pageable pageable);
    
    // 제목 또는 내용으로 검색 (모든 콘텐츠 타입)
    @Query("SELECT r FROM Review r WHERE r.title LIKE %:query% OR r.content LIKE %:query%")
    Page<Review> searchByTitleOrContent(@Param("query") String query, Pageable pageable);

    // 제목 또는 내용 및 콘텐츠 타입으로 검색
    @Query("SELECT r FROM Review r WHERE (r.title LIKE %:query% OR r.content LIKE %:query%) AND r.contentType = :contentType")
    Page<Review> searchByTitleOrContentAndContentType(@Param("query") String query, @Param("contentType") String contentType, Pageable pageable);

    // 특정 날짜 이후에 생성된 리뷰 조회
    List<Review> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 특정 기간 내 댓글 수가 많은 상위 리뷰를 조회합니다.
     * @param startDate 시작 날짜
     * @param pageable 페이징 정보 (limit 역할)
     * @return 인기 리뷰 목록
     */
    @Query("SELECT r FROM Review r WHERE r.createdAt >= :startDate ORDER BY r.commentCount DESC, r.createdAt DESC")
    List<Review> findHotReviews(@Param("startDate") LocalDateTime startDate, Pageable pageable);
    
    /**
     * movieId와 contentType으로 리뷰 목록을 조회합니다. (페이징 없음)
     * @param movieId 콘텐츠 ID
     * @param contentType 콘텐츠 타입
     * @return 리뷰 목록
     */
    List<Review> findByMovieIdAndContentType(Long movieId, String contentType);
} 