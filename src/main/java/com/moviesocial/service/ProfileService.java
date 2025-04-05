package com.moviesocial.service;

import com.moviesocial.payload.request.ProfileUpdateRequest;
import com.moviesocial.payload.response.ProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    /**
     * 사용자 프로필 정보를 가져옵니다.
     * @param username 사용자명
     * @return 사용자 프로필 정보
     */
    ProfileResponse getUserProfile(String username);
    
    /**
     * 사용자 프로필 정보를 가져옵니다. (팔로우 상태 포함)
     * @param username 조회할 사용자명
     * @param currentUsername 현재 로그인한 사용자명
     * @return 사용자 프로필 정보
     */
    ProfileResponse getUserProfileWithFollowStatus(String username, String currentUsername);
    
    /**
     * 사용자 프로필 정보를 ID로 가져옵니다. (팔로우 상태 포함)
     * @param userId 조회할 사용자 ID
     * @param currentUsername 현재 로그인한 사용자명
     * @return 사용자 프로필 정보
     */
    ProfileResponse getUserProfileByIdWithFollowStatus(Long userId, String currentUsername);
    
    /**
     * 사용자 프로필 정보를 업데이트합니다.
     * @param username 사용자명
     * @param request 업데이트할 프로필 정보
     */
    void updateProfile(String username, ProfileUpdateRequest request);
    
    /**
     * 프로필 이미지를 업로드합니다.
     * @param username 사용자명
     * @param file 업로드할 이미지 파일
     * @return 업로드된 이미지의 URL
     */
    String uploadProfileImage(String username, MultipartFile file);
} 