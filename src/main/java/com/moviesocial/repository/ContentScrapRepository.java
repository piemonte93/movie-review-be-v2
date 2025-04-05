package com.moviesocial.repository;

import com.moviesocial.model.ContentScrap;
import com.moviesocial.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 콘텐츠 스크랩 관련 데이터베이스 작업을 처리하는 리포지토리
 */
@Repository
public interface ContentScrapRepository extends JpaRepository<ContentScrap, Long> {
    
    /**
     * 사용자가 스크랩한 모든 콘텐츠를 생성일 기준 내림차순으로 가져옴
     * @param user 사용자 객체
     * @return 스크랩 목록
     */
    List<ContentScrap> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * 특정 사용자가 특정 콘텐츠를 스크랩했는지 확인
     * @param user 사용자 객체
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @return 스크랩 객체 (존재하는 경우)
     */
    Optional<ContentScrap> findByUserAndContentIdAndMediaType(User user, Long contentId, String mediaType);
    
    /**
     * 특정 사용자가 특정 콘텐츠를 스크랩했는지 여부 확인
     * @param user 사용자 객체
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     * @return 스크랩 존재 여부
     */
    boolean existsByUserAndContentIdAndMediaType(User user, Long contentId, String mediaType);
    
    /**
     * 특정 사용자의 특정 콘텐츠 스크랩 삭제
     * @param user 사용자 객체
     * @param contentId 콘텐츠 ID
     * @param mediaType 미디어 타입 (movie 또는 tv)
     */
    @Transactional
    void deleteByUserAndContentIdAndMediaType(User user, Long contentId, String mediaType);
} 