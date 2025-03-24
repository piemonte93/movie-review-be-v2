package com.moviesocial.service;

import com.moviesocial.model.PasswordResetToken;
import com.moviesocial.model.User;
import com.moviesocial.repository.PasswordResetTokenRepository;
import com.moviesocial.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.expiration}")
    private long resetTokenExpirationMs;

    /**
     * 비밀번호 재설정 요청을 처리하고 인증 코드를 이메일로 전송
     * 이메일 전송은 비동기로 처리되며, 즉시 응답을 반환합니다.
     */
    @Transactional
    public boolean requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", email);
            return false;
        }

        // 기존 사용하지 않은 토큰이 있으면 삭제
        tokenRepository.findByUserAndUsed(user, false)
                .ifPresent(tokenRepository::delete);

        // 인증 코드 생성 (6자리)
        String verificationCode = generateRandomCode(6);

        // 토큰 생성
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .verificationCode(verificationCode)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        tokenRepository.save(token);

        // 이메일 발송 (비동기)
        emailService.sendPasswordResetCode(user.getEmail(), verificationCode)
            .exceptionally(ex -> {
                log.error("이메일 전송 실패: {}", ex.getMessage());
                return false;
            });
        
        // 이메일 발송을 기다리지 않고 즉시 응답
        return true;
    }

    /**
     * 인증 코드 검증
     */
    @Transactional
    public String verifyCode(String email, String code) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        PasswordResetToken token = tokenRepository.findByUserAndUsed(user, false)
                .orElseThrow(() -> new RuntimeException("유효한 비밀번호 재설정 요청이 없습니다."));

        if (token.isExpired()) {
            throw new RuntimeException("인증 코드가 만료되었습니다.");
        }

        if (!token.getVerificationCode().equals(code)) {
            throw new RuntimeException("인증 코드가 일치하지 않습니다.");
        }

        return token.getToken();
    }

    /**
     * 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new RuntimeException("새 비밀번호를 입력해주세요.");
        }
        
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("유효한 토큰이 아닙니다."));

        if (resetToken.isExpired()) {
            throw new RuntimeException("토큰이 만료되었습니다.");
        }

        if (resetToken.isUsed()) {
            throw new RuntimeException("이미 사용된 토큰입니다.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    /**
     * 랜덤 인증 코드 생성
     */
    private String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }

        return code.toString();
    }
}