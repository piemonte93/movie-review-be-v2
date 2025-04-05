package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
} 