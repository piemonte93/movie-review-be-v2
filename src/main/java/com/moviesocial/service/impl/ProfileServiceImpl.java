package com.moviesocial.service.impl;

import com.moviesocial.model.Role;
import com.moviesocial.model.User;
import com.moviesocial.model.UserFollow;
import com.moviesocial.payload.request.ProfileUpdateRequest;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.repository.UserFollowRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.moviesocial.config.AppProperties;
import com.moviesocial.exception.FileStorageException;
import com.moviesocial.exception.ResourceNotFoundException;
import com.moviesocial.model.dto.UpdateProfileRequestDto;
import com.moviesocial.service.LocalFileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

@Service
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileServiceImpl.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserFollowRepository userFollowRepository;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Autowired
    private LocalFileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        
        // 팔로워, 팔로잉 수 조회
        long followerCount = userFollowRepository.countByFollowingId(user.getId());
        long followingCount = userFollowRepository.countByFollowerId(user.getId());
        
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .reviewCount(user.getReviews().size())
                .roles(roles)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(false)
                .followsMe(false)
                .mutualFollow(false)
                .build();
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfileWithFollowStatus(String username, String currentUsername) {
        // 같은 사용자인 경우
        if (username.equals(currentUsername)) {
            return getUserProfile(username);
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("현재 사용자를 찾을 수 없습니다: " + currentUsername));
        
        return buildProfileResponseWithFollowStatus(user, currentUser);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfileByIdWithFollowStatus(Long userId, String currentUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 같은 사용자인 경우
        if (user.getUsername().equals(currentUsername)) {
            return getUserProfile(user.getUsername());
        }
        
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("현재 사용자를 찾을 수 없습니다: " + currentUsername));
        
        return buildProfileResponseWithFollowStatus(user, currentUser);
    }
    
    /**
     * 팔로우 상태를 포함한 프로필 응답 생성
     */
    private ProfileResponse buildProfileResponseWithFollowStatus(User user, User currentUser) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        
        // 팔로워, 팔로잉 수 조회
        long followerCount = userFollowRepository.countByFollowingId(user.getId());
        long followingCount = userFollowRepository.countByFollowerId(user.getId());
        
        // 팔로우 상태 조회
        boolean isFollowing = userFollowRepository
                .findByFollowerIdAndFollowingId(currentUser.getId(), user.getId())
                .isPresent();
        
        boolean followsMe = userFollowRepository
                .findByFollowerIdAndFollowingId(user.getId(), currentUser.getId())
                .isPresent();
                
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .reviewCount(user.getReviews().size())
                .roles(roles)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(isFollowing)
                .followsMe(followsMe)
                .mutualFollow(isFollowing && followsMe)
                .build();
    }

    public void updateProfile(String username, ProfileUpdateRequest request) {
        logger.warn("Legacy updateProfile method called for user: {}. This might be deprecated.", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        if (request.getUsername() != null && !request.getUsername().isEmpty() 
                && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("이미 사용 중인 사용자명입니다: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getUserProfile(Long userId, UserDetails currentUserDetails) {
        logger.debug("Fetching profile for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // If currentUserDetails is available, check follow status
        if (currentUserDetails != null && !user.getUsername().equals(currentUserDetails.getUsername())) {
             User currentUser = userRepository.findByUsername(currentUserDetails.getUsername())
                     .orElseThrow(() -> new ResourceNotFoundException("Current User", "username", currentUserDetails.getUsername()));
             return buildProfileResponseWithFollowStatus(user, currentUser);
        } else {
             // Otherwise, return basic profile without follow status
             return buildProfileResponse(user);
        }
    }

    @Override
    @Transactional
    public User updateUserProfile(User user, UpdateProfileRequestDto profileData, MultipartFile imageFile) {
        logger.info("Updating profile for user ID: {}", user.getId());

        String oldImageUrl = user.getProfileImageUrl();
        String newImageUrl = oldImageUrl;

        if (imageFile != null && !imageFile.isEmpty()) {
            logger.info("New profile image provided: {}", imageFile.getOriginalFilename());
            if (!isValidImage(imageFile)) {
                 throw new FileStorageException("Invalid file type or size for profile image.");
            }

            try {
                newImageUrl = fileStorageService.storeFile(imageFile, user.getId());
                logger.info("New image stored. URL: {}", newImageUrl);
            } catch (IOException e) {
                logger.error("Failed to store profile image for user ID: {}", user.getId(), e);
                throw new FileStorageException("Could not store profile image.", e);
            }
        }

        boolean profileUpdated = false;
        if (!user.getUsername().equals(profileData.getUsername())) {
            logger.debug("Updating username from '{}' to '{}'", user.getUsername(), profileData.getUsername());
            user.setUsername(profileData.getUsername());
            profileUpdated = true;
        }
        String currentBio = user.getBio() == null ? "" : user.getBio();
        String newBio = profileData.getBio() == null ? "" : profileData.getBio();
        if (!currentBio.equals(newBio)) {
            logger.debug("Updating bio");
            user.setBio(profileData.getBio());
            profileUpdated = true;
        }
        if (newImageUrl != null && !newImageUrl.equals(oldImageUrl)) {
            logger.debug("Updating profile image URL from '{}' to '{}'", oldImageUrl, newImageUrl);
            user.setProfileImageUrl(newImageUrl);
            profileUpdated = true;
        }

        User savedUser = user;
        if (profileUpdated) {
            // Log the URL right before saving to DB
            logger.info("Saving updated profile for user ID: {}. Image URL being saved: '{}'", user.getId(), user.getProfileImageUrl()); 
            savedUser = userRepository.save(user);
        } else {
             logger.info("No changes detected in profile data for user ID: {}", user.getId());
        }

        if (newImageUrl != null && !newImageUrl.equals(oldImageUrl) && oldImageUrl != null && !oldImageUrl.trim().isEmpty() && !isDefaultImage(oldImageUrl)) {
             logger.info("Attempting to delete old profile image: {}", oldImageUrl);
             fileStorageService.deleteFile(oldImageUrl);
        } else if (newImageUrl != null && !newImageUrl.equals(oldImageUrl)) {
             logger.debug("Skipping deletion of old image. Old URL: '{}', New URL: '{}'", oldImageUrl, newImageUrl);
        }

        return savedUser;
    }

    private boolean isValidImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            logger.warn("Invalid content type: {}", contentType);
            return false;
        }
        return true;
    }

    private boolean isDefaultImage(String url) {
        if (url == null) return true;
        logger.trace("Checking if URL is default: {}", url);
        return false;
    }

    // Helper to build ProfileResponse without follow status
    private ProfileResponse buildProfileResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        long followerCount = userFollowRepository.countByFollowingId(user.getId());
        long followingCount = userFollowRepository.countByFollowerId(user.getId());
        int reviewCount = user.getReviews() != null ? user.getReviews().size() : 0; // Null check
        // Assuming postCount needs to be fetched or calculated similarly if required by ProfileResponse
        int postCount = 0; // Placeholder - calculate or fetch if needed

        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail()) // Consider privacy implications
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .reviewCount(reviewCount)
                .postCount(postCount) // Include if part of ProfileResponse
                .roles(roles)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(false) // Not applicable or false for basic profile
                .followsMe(false)
                .mutualFollow(false)
                .build();
    }
} 