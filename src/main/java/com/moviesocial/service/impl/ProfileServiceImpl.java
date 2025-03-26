package com.moviesocial.service.impl;

import com.moviesocial.model.Role;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.ProfileUpdateRequest;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.ProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.upload.dir:${user.home}/uploads}")
    private String uploadDir;
    
    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public ProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
        
        return ProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .bio(user.getBio())
                .reviewCount(user.getReviews().size())
                .roles(roles)
                .build();
    }

    @Override
    public void updateProfile(String username, ProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        // 사용자명 변경 요청이 있고, 현재 사용자명과 다른 경우
        if (request.getUsername() != null && !request.getUsername().isEmpty() 
                && !request.getUsername().equals(user.getUsername())) {
            // 중복 사용자명 체크
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("이미 사용 중인 사용자명입니다: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }
        
        // 소개글 업데이트
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        
        userRepository.save(user);
    }

    @Override
    public String uploadProfileImage(String username, MultipartFile file) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
        
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 파일 확장자 가져오기
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 고유한 파일명 생성
            String filename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(filename);
            
            // 파일 저장
            file.transferTo(filePath.toFile());
            
            // 상대 경로 생성 (프론트엔드에서 접근 가능한 URL)
            String imageUrl = "/api/uploads/" + filename;
            
            // 사용자 프로필 이미지 URL 업데이트
            user.setProfileImageUrl(imageUrl);
            userRepository.save(user);
            
            return imageUrl;
        } catch (IOException e) {
            log.error("프로필 이미지 업로드 중 오류 발생", e);
            throw new RuntimeException("프로필 이미지 업로드에 실패했습니다: " + e.getMessage());
        }
    }
} 