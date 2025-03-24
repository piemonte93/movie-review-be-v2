package com.moviesocial.service;

import com.moviesocial.model.EmailVerification;
import com.moviesocial.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;

    /**
     * 이메일 인증 코드 저장
     */
    @Transactional
    public void saveVerificationCode(String email, String code) {
        // 기존 인증 정보가 있으면 삭제
        emailVerificationRepository.findByEmailAndVerified(email, false)
                .ifPresent(emailVerificationRepository::delete);

        // 새 인증 정보 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiryDate(LocalDateTime.now().plusMinutes(10)) // 10분 유효
                .verified(false)
                .build();

        emailVerificationRepository.save(verification);
        log.info("이메일 인증 코드 생성 완료: {}", email);
    }

    /**
     * 인증 코드 검증
     */
    @Transactional
    public boolean verifyCode(String email, String code) {
        EmailVerification verification = emailVerificationRepository.findByEmailAndVerified(email, false)
                .orElse(null);

        if (verification == null) {
            log.warn("이메일 인증 정보가 없음: {}", email);
            return false;
        }

        if (verification.isExpired()) {
            log.warn("이메일 인증 코드 만료됨: {}", email);
            return false;
        }

        if (!verification.getVerificationCode().equals(code)) {
            log.warn("이메일 인증 코드 불일치: {}", email);
            return false;
        }

        // 인증 완료 처리
        verification.setVerified(true);
        emailVerificationRepository.save(verification);
        log.info("이메일 인증 완료: {}", email);

        return true;
    }

    /**
     * 인증 여부 확인
     */
    public boolean isEmailVerified(String email) {
        return emailVerificationRepository.findByEmailAndVerified(email, true)
                .isPresent();
    }

    /**
     * 랜덤 인증 코드 생성
     */
    public String generateRandomCode(int length) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10)); // 0-9 숫자
        }
        
        return sb.toString();
    }
} 