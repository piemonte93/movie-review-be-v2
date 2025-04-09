package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.model.dto.UpdateProfileRequestDto;
import com.moviesocial.payload.response.ProfileResponse;
import com.moviesocial.payload.response.ReviewResponse;
import com.moviesocial.service.ProfileService;
import com.moviesocial.service.ReviewService;
import com.moviesocial.service.ScrapService;
import com.moviesocial.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moviesocial.repository.UserRepository;
import com.moviesocial.repository.UserFollowRepository;
import com.moviesocial.security.services.UserDetailsImpl;
import com.moviesocial.security.services.UserDetailsServiceImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.moviesocial.exception.FileStorageException;
import com.moviesocial.exception.ResourceNotFoundException;
import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    @Autowired
    private ProfileService profileService;
    
    @Autowired
    private ReviewService reviewService;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ScrapService scrapService;

    @Autowired
    private UserFollowRepository userFollowRepository;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @GetMapping("/{username}")
    public ResponseEntity<ProfileResponse> getUserProfile(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("사용자 프로필 조회 요청 - 대상: {}", username);
        
        if (userDetails == null) {
            // 로그인하지 않은 사용자는 기본 프로필 정보만 조회
            return ResponseEntity.ok(profileService.getUserProfile(username));
        }
        
        // 팔로우 상태를 포함한 프로필 정보 반환
        ProfileResponse profile = profileService.getUserProfileWithFollowStatus(username, userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ProfileResponse> getUserProfileById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("ID로 사용자 프로필 조회 요청 - 대상 ID: {}", id);
        
        // ID로 사용자 조회
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 ID의 사용자를 찾을 수 없습니다: " + id));
        
        if (userDetails == null) {
            // 로그인하지 않은 사용자는 기본 프로필 정보만 조회
            return ResponseEntity.ok(profileService.getUserProfile(user.getUsername()));
        }
        
        // 팔로우 상태를 포함한 프로필 정보 반환
        ProfileResponse profile = profileService.getUserProfileByIdWithFollowStatus(id, userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ProfileResponse> getMyProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            // 이 경우는 @PreAuthorize 때문에 거의 발생하지 않지만 안전하게 처리
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        log.info("내 프로필 조회 요청 - 사용자 ID: {}, 사용자 이름: {}", userDetails.getId(), userDetails.getUsername());

        // @AuthenticationPrincipal 에서 직접 정보 가져오기 (DB 재조회 방지)
        Long userId = userDetails.getId();
        String username = userDetails.getUsername();
        String email = userDetails.getEmail();
        // UserDetailsImpl에 profileImageUrl, bio 필드가 있다면 가져오기 (없으면 null 또는 기본값)
        String profileImageUrl = userDetails.getProfileImageUrl(); // UserDetailsImpl에 getter 추가 필요 가정
        String bio = userDetails.getBio(); // UserDetailsImpl에 getter 추가 필요 가정

        // 역할 정보 추출
        List<String> roles = userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toList());

        // 팔로워, 팔로잉 수 조회
        long followerCount = userFollowRepository.countByFollowingId(userId);
        long followingCount = userFollowRepository.countByFollowerId(userId);

        // 리뷰 수 조회 (이 부분은 여전히 DB 조회가 필요할 수 있음. UserDetailsImpl에 리뷰 정보가 없다면)
        // User 객체를 로드해야 할 수도 있지만, 우선 UserDetails 정보만 사용
        int reviewCount = 0; // UserDetailsImpl에 리뷰 카운트 정보가 없다면 0 또는 다른 방식으로 조회 필요
        // Optional<User> userOptional = userRepository.findById(userId);
        // if (userOptional.isPresent()) {
        //    reviewCount = userOptional.get().getReviews().size(); 
        // }

        // ProfileResponse 직접 생성
        ProfileResponse profile = ProfileResponse.builder()
                .id(userId)
                .username(username)
                .email(email)
                .profileImageUrl(profileImageUrl)
                .bio(bio)
                .reviewCount(reviewCount) // 리뷰 수는 별도 조회 또는 UserDetailsImpl 확장 필요
                .roles(roles)
                .followerCount(followerCount)
                .followingCount(followingCount)
                .isFollowing(false) // 내 프로필이므로 항상 false
                .followsMe(false)  // 내 프로필이므로 항상 false
                .mutualFollow(false) // 내 프로필이므로 항상 false
                .build();

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{username}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getUserReviews(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String contentType) {
        
        log.info("사용자 리뷰 조회 요청 - 대상: {}, 페이지: {}, 사이즈: {}, 콘텐츠 타입: {}", 
                username, page, size, contentType);
        
        if (contentType != null && !contentType.isEmpty()) {
            return ResponseEntity.ok(reviewService.getUserReviews(username, page, size, contentType));
        } else {
            return ResponseEntity.ok(reviewService.getUserReviews(username, page, size));
        }
    }

    @GetMapping("/me/reviews")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("내 리뷰 조회 요청 - 사용자: {}, 페이지: {}, 사이즈: {}", 
                userDetails.getUsername(), page, size);
        
        return ResponseEntity.ok(reviewService.getUserReviews(userDetails.getUsername(), page, size));
    }

    @GetMapping("/{username}/scraps")
    public ResponseEntity<?> getUserScraps(@PathVariable String username) {
        try {
            log.info("사용자 스크랩 조회 요청 - 대상: {}", username);
            
            // 유저 확인 로직 제거 - 접근 제한 완화
            return ResponseEntity.ok(scrapService.getUserScraps(username));
        } catch (Exception e) {
            log.error("스크랩 목록 조회 실패 - 대상: {}, 오류: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("스크랩 목록을 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{username}/activity")
    public ResponseEntity<?> getUserActivity(@PathVariable String username) {
        try {
            log.info("사용자 활동 조회 요청 - 대상: {}", username);
            
            // 사용자가 존재하는지 확인
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("해당 사용자를 찾을 수 없습니다: " + username));
            
            // 더미 활동 데이터 반환 (실제 구현 필요)
            Map<String, Object> activityData = new HashMap<>();
            activityData.put("favoriteMovies", new ArrayList<>());
            activityData.put("favoriteReviews", new ArrayList<>());
            activityData.put("favoritePosts", new ArrayList<>());
            
            return ResponseEntity.ok(activityData);
        } catch (Exception e) {
            log.error("활동 정보 조회 실패 - 대상: {}, 오류: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("활동 정보를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * Updates the authenticated user's profile information, including optional profile image.
     * Expects multipart/form-data with 'profileData' (JSON) and optionally 'imageFile'.
     *
     * @param userDetails Authenticated user details.
     * @param profileData DTO containing username and bio (validated).
     * @param imageFile Optional new profile image.
     * @return ResponseEntity containing the updated user profile information.
     */
    @PutMapping(value = "/me", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<?> updateMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestPart("profileData") @Valid UpdateProfileRequestDto profileData,
            @RequestPart(value = "imageFile", required = false) MultipartFile imageFile) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required.");
        }

        try {
            log.info("Received profile update request for user ID: {}", userDetails.getId());
            if (imageFile != null && !imageFile.isEmpty()) {
                log.info("Image file received: {}, size: {} bytes, type: {}",
                        imageFile.getOriginalFilename(), imageFile.getSize(), imageFile.getContentType());
            }

            // Fetch the current user entity before passing to service
            User currentUser = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));

            // Call the service to update the profile (returns updated User entity)
            User updatedUserEntity = profileService.updateUserProfile(currentUser, profileData, imageFile);

            // --- 추가: SecurityContext 업데이트 ---
            // 업데이트된 사용자 정보로 UserDetails 다시 로드
            UserDetails updatedUserDetails = userDetailsService.loadUserByUsername(updatedUserEntity.getUsername());
            // 새로운 Authentication 객체 생성
            UsernamePasswordAuthenticationToken newAuthentication = new UsernamePasswordAuthenticationToken(
                    updatedUserDetails, null, updatedUserDetails.getAuthorities());
            // 현재 Security Context에 새로운 Authentication 설정
            SecurityContextHolder.getContext().setAuthentication(newAuthentication);
            log.info("SecurityContext updated for user: {}", updatedUserEntity.getUsername());
            // --- 추가 끝 ---

            // --- 추가: 사용자 이름 변경 시 새 JWT 토큰 생성 ---
            String newToken = null;
            boolean usernameChanged = !currentUser.getUsername().equals(updatedUserEntity.getUsername());
            
            if (usernameChanged) {
                // 새 JWT 토큰 생성
                newToken = jwtUtils.generateJwtToken(newAuthentication);
                log.info("Username changed from '{}' to '{}'. New JWT token generated.", 
                        currentUser.getUsername(), updatedUserEntity.getUsername());
                        
                // 토큰 내용 디버깅
                try {
                    String username = jwtUtils.getUserNameFromJwtToken(newToken);
                    log.info("새 토큰의 사용자명 확인: {}", username);
                    log.info("새 토큰의 길이: {}", newToken.length());
                    log.info("새 토큰 샘플: {}...", newToken.substring(0, 20));
                } catch (Exception e) {
                    log.error("토큰 파싱 중 오류: {}", e.getMessage());
                }
            }
            // --- 추가 끝 ---

            // updatedUserEntity를 기반으로 ProfileResponse 생성
            // (buildProfileResponse 또는 buildProfileResponseWithFollowStatus 유사 로직 필요)
            List<String> roles = updatedUserEntity.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());
            long followerCount = userFollowRepository.countByFollowingId(updatedUserEntity.getId());
            long followingCount = userFollowRepository.countByFollowerId(updatedUserEntity.getId());

            ProfileResponse response = ProfileResponse.builder()
                 .id(updatedUserEntity.getId())
                 .username(updatedUserEntity.getUsername())
                 .email(updatedUserEntity.getEmail())
                 .profileImageUrl(updatedUserEntity.getProfileImageUrl())
                 .bio(updatedUserEntity.getBio())
                 .reviewCount(updatedUserEntity.getReviews() != null ? updatedUserEntity.getReviews().size() : 0) // Null check 추가
                 .postCount(0) // TODO: 실제 포스트 수 조회 로직 추가 필요
                 .roles(roles)
                 .followerCount(followerCount)
                 .followingCount(followingCount)
                 // 내 프로필 업데이트 후 응답이므로 팔로우 관련은 false
                 .isFollowing(false)
                 .followsMe(false)
                 .mutualFollow(false)
                 .build();

            // 로그 추가: 업데이트된 정보 확인
            log.info("Successfully updated profile for user ID: {}. Responding with: {}", updatedUserEntity.getId(), response);

            // --- 추가: 사용자 이름 변경 시 새 토큰 포함한 응답 반환 ---
            if (usernameChanged && newToken != null) {
                // 토큰과 프로필 정보를 모두 포함하는 응답 객체 생성
                Map<String, Object> responseWithToken = new HashMap<>();
                responseWithToken.put("profile", response);
                responseWithToken.put("accessToken", newToken);
                responseWithToken.put("tokenType", "Bearer");
                
                log.info("Returning response with new JWT token for username change");
                
                // 응답 구조 디버깅
                try {
                    log.info("응답 객체 구조: {}", responseWithToken.keySet());
                    log.info("응답 accessToken 존재 여부: {}", responseWithToken.containsKey("accessToken"));
                    log.info("응답 profile 존재 여부: {}", responseWithToken.containsKey("profile"));
                } catch (Exception e) {
                    log.error("응답 객체 디버깅 중 오류: {}", e.getMessage());
                }
                
                return ResponseEntity.ok(responseWithToken);
            } else {
                // 일반적인 경우 프로필 정보만 반환
                return ResponseEntity.ok(response);
            }
            // --- 추가 끝 ---

        } catch (FileStorageException e) {
            log.error("File storage error during profile update for user ID: {}. Error: {}", userDetails.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File storage error: " + e.getMessage());
        } catch (ResourceNotFoundException e) {
            log.error("Resource not found during profile update for user ID: {}. Error: {}", userDetails.getId(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (RuntimeException e) {
            // username 중복 또는 다른 Runtime 예외 처리
            log.error("Error updating profile for user ID: {}. Error: {}", userDetails.getId(), e.getMessage(), e);
            // 클라이언트에게 더 친화적인 메시지 제공 고려
            String errorMessage = "An error occurred while updating the profile.";
            if (e.getMessage() != null && e.getMessage().contains("이미 사용 중인 사용자명입니다")) {
                 errorMessage = e.getMessage(); // 중복 메시지 그대로 전달
                 return ResponseEntity.status(HttpStatus.CONFLICT).body(errorMessage);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (Exception e) {
            log.error("Unexpected error updating profile for user ID: {}. Error: {}", userDetails.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
} 