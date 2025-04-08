package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.NotificationResponse;
import com.moviesocial.service.NotificationService;
import com.moviesocial.security.services.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.security.jwt.JwtUtils;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;
    
    // SSE 연결을 저장하기 위한 Map (사용자 ID -> SSE Emitter)
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // SSE 연결 설정 엔드포인트
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            // 사용자 미인증 상태로 빈 emitter 반환
            SseEmitter emitter = new SseEmitter(1800000L);
            try {
                emitter.send(SseEmitter.event()
                        .name("connect")
                        .data("Connected without authentication"));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }
        
        Long userId = currentUser.getId();
        
        // 이전 연결이 있으면 완료 처리
        SseEmitter existingEmitter = emitters.get(userId);
        if (existingEmitter != null) {
            existingEmitter.complete();
        }
        
        // 새 SSE 연결 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(1800000L);
        
        // 연결 종료 시 처리
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));
        
        // 초기 연결 확인 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("Connected successfully"));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        
        // 사용자 ID와 Emitter 매핑 저장
        emitters.put(userId, emitter);
        
        return emitter;
    }
    
    // 내 알림 목록 조회
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UsernameNotFoundException("로그인이 필요합니다.");
        }
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getNotifications(currentUser.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }
    
    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UsernameNotFoundException("로그인이 필요합니다.");
        }
        
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(count);
    }
    
    // 알림 읽음 처리
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UsernameNotFoundException("로그인이 필요합니다.");
        }
        
        NotificationResponse notification = notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(notification);
    }
    
    // 모든 알림 읽음 처리
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new UsernameNotFoundException("로그인이 필요합니다.");
        }
        
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok().build();
    }
    
    // 특정 사용자에게 SSE 이벤트 전송
    public void sendNotification(Long userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }
    
    // 현재 인증된 사용자 정보 가져오기
    private User getCurrentUser() {
        try {
            UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return userDetails.getUser();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 현재 인증된 사용자의 모든 알림 삭제
     * @param userDetails 인증된 사용자 정보
     * @return 성공 응답 (No Content)
     */
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteAllNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String username = userDetails.getUsername();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        notificationService.deleteAllNotifications(currentUser.getId());
        return ResponseEntity.noContent().build(); // 204 No Content
    }
} 