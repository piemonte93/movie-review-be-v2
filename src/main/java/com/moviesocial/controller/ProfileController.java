package com.moviesocial.controller;

import com.moviesocial.model.User;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.moviesocial.repository.UserRepository;
import com.moviesocial.repository.UserFollowRepository;
import com.moviesocial.security.services.UserDetailsImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
} 