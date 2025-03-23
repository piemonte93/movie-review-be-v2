package com.moviesocial.service;

import java.util.concurrent.CompletableFuture;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * 비동기로 인증 코드를 이메일로 전송합니다.
     * @param to 수신자 이메일
     * @param code 인증 코드
     * @return 작업 완료를 알리는 CompletableFuture
     */
    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVerificationCode(String to, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Movie Review 비밀번호 재설정 인증 코드");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
                        <h2 style="color: #333; border-bottom: 1px solid #e0e0e0; padding-bottom: 10px;">비밀번호 재설정 인증 코드</h2>
                        <p>안녕하세요,</p>
                        <p>아래의 인증 코드를 입력하여 비밀번호 재설정을 완료해주세요:</p>
                        <div style="background-color: #f7f7f7; padding: 15px; border-radius: 4px; margin: 20px 0; text-align: center;">
                            <span style="font-size: 24px; font-weight: bold; letter-spacing: 3px;">%s</span>
                        </div>
                        <p>이 코드는 10분 동안 유효합니다.</p>
                        <p>비밀번호 재설정 요청을 하지 않으셨다면, 이 이메일을 무시하셔도 됩니다.</p>
                        <p style="margin-top: 20px; font-size: 12px; color: #777; border-top: 1px solid #e0e0e0; padding-top: 10px;">
                            본 이메일은 자동 발송되었습니다. 회신하지 마세요.
                        </p>
                    </div>
                    """.formatted(code);

            helper.setText(htmlContent, true);
            mailSender.send(message);

            log.info("Verification email sent to: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", to, e);
            return CompletableFuture.failedFuture(
                new RuntimeException("이메일 전송 중 오류가 발생했습니다.", e)
            );
        }
    }
}