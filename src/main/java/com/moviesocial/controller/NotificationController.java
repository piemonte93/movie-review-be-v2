package com.moviesocial.controller;

import com.moviesocial.model.User;
import com.moviesocial.payload.response.NotificationResponse;
import com.moviesocial.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    
    // 내 알림 목록 조회
    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> notifications = notificationService.getNotifications(currentUser.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }
    
    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(count);
    }
    
    // 알림 읽음 처리
    @PutMapping("/{id}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        
        NotificationResponse notification = notificationService.markAsRead(id, currentUser.getId());
        return ResponseEntity.ok(notification);
    }
    
    // 모든 알림 읽음 처리
    @PutMapping("/read-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok().build();
    }
} 