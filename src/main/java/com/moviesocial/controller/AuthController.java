package com.moviesocial.controller;

import com.moviesocial.model.ERole;
import com.moviesocial.model.Role;
import com.moviesocial.model.User;
import com.moviesocial.payload.request.*;
import com.moviesocial.payload.response.JwtResponse;
import com.moviesocial.payload.response.MessageResponse;
import com.moviesocial.payload.response.TokenResponse;
import com.moviesocial.repository.RoleRepository;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.security.jwt.JwtUtils;
import com.moviesocial.security.services.UserDetailsImpl;
import com.moviesocial.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.moviesocial.service.EmailVerificationService;
import com.moviesocial.service.EmailService;
import com.moviesocial.payload.response.CheckAvailabilityResponse;
import com.moviesocial.payload.request.EmailVerificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @Autowired
    PasswordResetService passwordResetService;
    
    @Value("${app.oauth2.redirectUri:http://localhost:5173/oauth2/redirect}")
    private String redirectUri;
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;
    
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;
    
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Autowired
    private EmailVerificationService emailVerificationService;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // 사용자 확인
            User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 소셜 로그인 계정인 경우
            if (user.isSocialLogin()) {
                return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("이 계정은 구글 로그인으로 가입된 계정입니다. 구글 로그인을 이용해주세요."));
            }
            
            // 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            // 인증 성공 시 SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // JWT 토큰 생성
            String jwt = jwtUtils.generateJwtToken(authentication);

            // 사용자 상세 정보 가져오기
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            // 사용자 권한 목록
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            // 응답 생성
            return ResponseEntity.ok(new JwtResponse(jwt,
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles));
        } catch (RuntimeException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("로그인에 실패했습니다. 이메일과 비밀번호를 확인해주세요."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody SignupRequest signUpRequest) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Email is already in use!"));
        }

        // 사용자 이름 중복 체크
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error: Username is already taken!"));
        }

        // 새 사용자 계정 생성
        User user = new User();
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setSocialLogin(false); // 일반 회원가입 - 소셜 로그인 아님

        // 권한 설정
        Set<Role> roles = new HashSet<>();

        // 기본적으로 USER 권한 부여
        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        roles.add(userRole);

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        boolean result = passwordResetService.requestPasswordReset(request.getEmail());

        if (result) {
            return ResponseEntity.ok(new MessageResponse("인증 코드가 이메일로 전송되었습니다."));
        } else {
            // 실패했더라도 보안을 위해 성공한 것처럼 응답
            return ResponseEntity.ok(new MessageResponse("인증 코드가 이메일로 전송되었습니다."));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyCodeRequest request) {
        try {
            String token = passwordResetService.verifyCode(request.getEmail(), request.getCode());
            return ResponseEntity.ok(new TokenResponse(token));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            // 요청 본문 로깅
            System.out.println("요청 데이터: " + request);
            System.out.println("토큰: " + request.getToken() + ", 비밀번호: " + (request.getNewPassword() != null ? "******" : "null"));
            
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(new MessageResponse("비밀번호가 성공적으로 재설정되었습니다."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Google OAuth2 인증 시작 엔드포인트
     * 프론트엔드에서 이 URL로 리다이렉트하면 구글 로그인 페이지로 이동
     */
    @GetMapping("/oauth2/authorize/google")
    public RedirectView authorizeGoogle(@RequestParam(value = "redirect_uri", required = false) String redirectUriParam) {
        // 구글 OAuth 인증 URL
        String googleOAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth";
        
        // OAuth 파라미터 - 환경 변수에서 가져온 값 사용
        String clientId = googleClientId;
        String responseType = "code";
        String scope = "email profile";
        
        // 리다이렉트 URI는 콜백 URL이어야 함
        // 여기서는 Google 콘솔에 등록된 콜백 URL을 사용해야 함
        String finalRedirectUri = "http://localhost:8080/api/auth/oauth2/callback/google";
        
        // 프론트엔드 리다이렉트 URI를 상태 파라미터로 전달
        String state = redirectUriParam != null ? redirectUriParam : redirectUri;
        
        // 구글 로그인 URL 생성
        String authorizationUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&response_type=%s&scope=%s&state=%s",
            googleOAuthUrl, clientId, finalRedirectUri, responseType, scope, state
        );
        
        System.out.println("리다이렉트 URL: " + authorizationUrl);
        
        return new RedirectView(authorizationUrl);
    }
    
    /**
     * Google OAuth2 콜백 처리 엔드포인트
     * 구글 로그인 후 이 URL로 리다이렉트됨
     */
    @GetMapping("/oauth2/callback/google")
    public RedirectView oauthCallback(@RequestParam("code") String code, @RequestParam(value = "state", required = false) String state) throws IOException {
        // 디버깅을 위한 코드 출력
        System.out.println("Google에서 받은 인증 코드: " + code);
        System.out.println("전달받은 state 값: " + state);
        
        try {
            // 토큰 교환 요청 준비 - 환경 변수에서 가져온 값 사용
            String tokenUrl = "https://oauth2.googleapis.com/token";
            String clientId = googleClientId;
            String clientSecret = googleClientSecret;
            String redirectUri = "http://localhost:8080/api/auth/oauth2/callback/google"; // 구글 콘솔에 등록된 URI와 일치해야 함
            
            // RestTemplate을 사용하여 토큰 교환 요청
            RestTemplate restTemplate = new RestTemplate();
            
            // 요청 바디 준비
            MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
            requestBody.add("code", code);
            requestBody.add("client_id", clientId);
            requestBody.add("client_secret", clientSecret);
            requestBody.add("redirect_uri", redirectUri);
            requestBody.add("grant_type", "authorization_code");
            
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            // HTTP 요청 엔티티 생성
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // 토큰 교환 요청 실행
            ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                requestEntity,
                Map.class
            );
            
            // 응답에서 액세스 토큰 추출
            Map<String, Object> responseBody = response.getBody();
            String accessToken = (String) responseBody.get("access_token");
            
            // Google API를 사용하여 사용자 정보 가져오기
            String userInfoUrl = "https://www.googleapis.com/oauth2/v3/userinfo";
            HttpHeaders userInfoHeaders = new HttpHeaders();
            userInfoHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<String> userInfoRequestEntity = new HttpEntity<>(userInfoHeaders);
            
            ResponseEntity<Map> userInfoResponse = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.GET,
                userInfoRequestEntity,
                Map.class
            );
            
            // 사용자 정보 추출
            Map<String, Object> userInfo = userInfoResponse.getBody();
            String email = (String) userInfo.get("email");
            String name = (String) userInfo.get("name");
            
            // 이미 존재하는 사용자인지 확인
            Optional<User> existingUserOpt = userRepository.findByEmail(email);
            
            // 일반 회원가입으로 가입된 계정인 경우
            if (existingUserOpt.isPresent() && !existingUserOpt.get().isSocialLogin()) {
                // 일반 회원가입 계정으로 이미 존재함을 알리는 오류 페이지로 리다이렉트
                return new RedirectView(frontendUrl + "/oauth2/redirect?error=email_exists_regular_account");
            }
            
            // 기존 사용자인지 새 사용자인지 판단하기 위한 변수
            boolean userExists = userRepository.existsByEmail(email);
            // 새로운 변수로 분리하여 람다 내부에서 사용할 수 있게 함
            final boolean[] isNewUserFlag = {false};
            
            System.out.println("사용자 이메일: " + email);
            System.out.println("사용자가 이미 존재하는지: " + userExists);
            
            // 사용자 정보로 회원가입 또는 로그인 처리
            User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    // 신규 사용자 등록
                    System.out.println("새 사용자 등록 - 이메일: " + email);
                    User newUser = new User();
                    newUser.setEmail(email);
                    // 초기 닉네임을 이메일로 설정 - 이후 프론트엔드에서 변경
                    newUser.setUsername(email);
                    newUser.setPassword(encoder.encode("OAUTH2_" + System.currentTimeMillis())); // 임의 패스워드
                    newUser.setSocialLogin(true); // 소셜 로그인으로 설정
                    
                    // 권한 설정
                    Set<Role> roles = new HashSet<>();
                    Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                    roles.add(userRole);
                    newUser.setRoles(roles);
                    
                    // 새 사용자 생성 후 isNewUser를 true로 설정
                    isNewUserFlag[0] = true;
                    
                    // 새 사용자 플래그 설정
                    return userRepository.save(newUser);
                });
            
            // 새 사용자 여부 체크 (닉네임이 이메일과 같은 경우)
            boolean isNewUser = isNewUserFlag[0];
            if (!isNewUser) {
                isNewUser = user.getUsername().equals(user.getEmail());
            }
            
            System.out.println("최종 isNewUser 값: " + isNewUser);
            System.out.println("사용자 닉네임: " + user.getUsername());
            System.out.println("사용자 이메일: " + user.getEmail());
            
            // JWT 토큰 생성
            UserDetailsImpl userDetails = UserDetailsImpl.build(user);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            // 프론트엔드로 리다이렉트 (토큰과 새 사용자 여부 포함)
            // state 파라미터가 있는 경우 해당 URL로 리다이렉트, 없는 경우 기본 리다이렉트 URL 사용
            String redirectUrl = (state != null && !state.isEmpty()) ? state : frontendUrl + "/oauth2/redirect";
            return new RedirectView(redirectUrl + "?token=" + jwt + "&isNewUser=" + isNewUser);
        } catch (Exception e) {
            // 오류 발생 시 에러 페이지로 리다이렉트
            System.err.println("OAuth2 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            String redirectUrl = (state != null && !state.isEmpty()) ? state : frontendUrl + "/oauth2/redirect";
            return new RedirectView(redirectUrl + "?error=authentication_failed");
        }
    }
    
    /**
     * JWT 토큰으로 사용자 정보 조회
     */
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            // "Bearer " 부분 제거
            String token = authHeader.substring(7);
            
            // 토큰 검증
            if (jwtUtils.validateJwtToken(token)) {
                String email = jwtUtils.getEmailFromJwtToken(token);
                User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
                
                // 사용자 권한 목록
                List<String> roles = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toList());
                
                // 새 사용자 여부 체크 (닉네임이 이메일과 같은 경우)
                boolean isNewUser = user.getUsername().equals(user.getEmail());
                
                System.out.println("user-info API - 사용자 이메일: " + user.getEmail());
                System.out.println("user-info API - 사용자 닉네임: " + user.getUsername());
                System.out.println("user-info API - isNewUser: " + isNewUser);
                
                // 응답 생성
                JwtResponse response = new JwtResponse(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    roles
                );
                
                // 새 사용자 여부 추가
                response.setIsNewUser(isNewUser);
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("유효하지 않은 토큰입니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("토큰 처리 중 오류: " + e.getMessage()));
        }
    }
    
    /**
     * 닉네임 중복 확인 API
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsername(@RequestParam String username) {
        try {
            boolean exists = userRepository.existsByUsername(username);
            return ResponseEntity.ok(new CheckAvailabilityResponse(!exists));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("닉네임 중복 확인 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * 닉네임 업데이트 API
     */
    @PostMapping("/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, String> request, @RequestHeader("Authorization") String authHeader) {
        try {
            // 이전 토큰 검증 및 사용자 정보 로드 (기존 로직 유지)
            String oldToken = authHeader.substring(7);
            if (!jwtUtils.validateJwtToken(oldToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("유효하지 않은 토큰입니다."));
            }
            String email = jwtUtils.getEmailFromJwtToken(oldToken);
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 닉네임 중복 확인 (기존 로직 유지)
            String newUsername = request.get("username");
            if (userRepository.existsByUsername(newUsername) && !user.getUsername().equals(newUsername)) {
                return ResponseEntity.badRequest().body(new MessageResponse("이미 사용 중인 닉네임입니다."));
            }

            // 닉네임 업데이트 및 DB 저장 (기존 로직 유지)
            user.setUsername(newUsername);
            User updatedUser = userRepository.save(user); // 저장 후 반환된 객체 사용

            // --- 새로운 JWT 토큰 생성 로직 시작 ---
            // 1. 업데이트된 사용자 정보로 UserDetailsImpl 생성
            UserDetailsImpl newUserDetails = UserDetailsImpl.build(updatedUser);

            // 2. 새로운 Authentication 객체 생성 (비밀번호는 null 또는 빈 문자열)
            //    UsernamePasswordAuthenticationToken을 사용하거나, 다른 적절한 Authentication 구현체 사용 가능
            //    여기서는 토큰 발급 목적이므로 principal과 authorities만 중요함
            Authentication newAuthentication = new UsernamePasswordAuthenticationToken(
                newUserDetails, 
                null, // 비밀번호는 필요 없음
                newUserDetails.getAuthorities()
            );
            
            // 3. 새로운 JWT 토큰 생성
            String newJwt = jwtUtils.generateJwtToken(newAuthentication);
            // --- 새로운 JWT 토큰 생성 로직 끝 ---

            // 사용자 권한 목록 (이미 newUserDetails에 포함되어 있지만, 응답 형식을 위해 재생성)
            List<String> roles = newUserDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

            // 응답 생성: 새로운 토큰과 업데이트된 사용자 정보 포함
            return ResponseEntity.ok(new JwtResponse(
                newJwt, // <-- 새로 생성된 토큰 사용
                updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail(),
                roles
            ));
        } catch (Exception e) {
            log.error("닉네임 업데이트 중 오류 발생", e); // 로깅 추가
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("닉네임 업데이트 중 오류 발생: " + e.getMessage()));
        }
    }
    
    /**
     * 신규 OAuth 사용자 계정 삭제 API
     * 닉네임 설정을 취소할 경우 임시로 생성된 계정 삭제
     */
    @DeleteMapping("/cancel-oauth-signup")
    public ResponseEntity<?> cancelOAuthSignup(@RequestHeader("Authorization") String authHeader) {
        try {
            // 토큰에서 사용자 정보 추출
            String token = authHeader.substring(7);
            
            if (!jwtUtils.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse("유효하지 않은 토큰입니다."));
            }
            
            String email = jwtUtils.getEmailFromJwtToken(token);
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
            
            // 신규 사용자 여부 확인 (닉네임이 이메일과 같은지)
            boolean isNewUser = user.getUsername().equals(user.getEmail());
            
            if (isNewUser) {
                // 신규 사용자만 삭제 가능
                System.out.println("신규 가입 취소 - 사용자 삭제: " + email);
                userRepository.delete(user);
                return ResponseEntity.ok(new MessageResponse("회원가입이 취소되었습니다."));
            } else {
                // 이미 닉네임이 설정된 기존 사용자는 삭제 불가
                return ResponseEntity.badRequest().body(new MessageResponse("이미 가입된 사용자는 삭제할 수 없습니다."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new MessageResponse("계정 삭제 중 오류 발생: " + e.getMessage()));
        }
    }

    // 이메일 중복 체크 API - 새 엔드포인트
    @GetMapping("/public/check-email")
    public ResponseEntity<?> publicCheckEmailAvailability(@RequestParam String email) {
        Boolean isAvailable = !userRepository.existsByEmail(email);
        return ResponseEntity.ok(new CheckAvailabilityResponse(isAvailable));
    }

    // 이메일 인증 코드 전송 API
    @PostMapping("/send-verification-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody EmailVerificationRequest request) {
        String email = request.getEmail();
        
        // 6자리 랜덤 코드 생성
        String verificationCode = emailVerificationService.generateRandomCode(6);
        
        // 인증 코드 저장 (세션 또는 DB)
        emailVerificationService.saveVerificationCode(email, verificationCode);
        
        // 이메일 발송
        emailService.sendVerificationCode(email, verificationCode)
                .exceptionally(ex -> {
                    log.error("이메일 전송 실패: {}", ex.getMessage());
                    return false;
                });
                
        return ResponseEntity.ok(new MessageResponse("인증 코드가 이메일로 전송되었습니다."));
    }
    
    // 이메일 인증 코드 확인 API
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody EmailVerificationRequest request) {
        String email = request.getEmail();
        String code = request.getCode();
        
        boolean isValid = emailVerificationService.verifyCode(email, code);
        
        if (!isValid) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("잘못된 인증 코드입니다."));
        }
        
        return ResponseEntity.ok(new MessageResponse("이메일 인증이 완료되었습니다."));
    }
}