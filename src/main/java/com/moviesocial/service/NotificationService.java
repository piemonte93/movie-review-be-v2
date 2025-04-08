package com.moviesocial.service;

import com.moviesocial.model.*;
import com.moviesocial.payload.response.NotificationResponse;
import com.moviesocial.repository.NotificationRepository;
import com.moviesocial.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        Page<Notification> notifications = notificationRepository.findByToUserOrderByCreatedAtDesc(user, pageable);
        
        return notifications.map(this::convertToResponse);
    }
    
    public long getUnreadCount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        return notificationRepository.countByToUserAndReadFalse(user);
    }
    
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다. ID: " + notificationId));
        
        if (!notification.getToUser().getId().equals(userId)) {
            throw new RuntimeException("이 알림에 대한 권한이 없습니다.");
        }
        
        notification.setRead(true);
        Notification updatedNotification = notificationRepository.save(notification);
        
        return convertToResponse(updatedNotification);
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        notificationRepository.markAllAsRead(userId);
    }
    
    @Transactional
    public void createNotification(User fromUser, User toUser, Post post, Comment comment, Notification.NotificationType type) {
        if (fromUser.getId().equals(toUser.getId())) return;
        
        Notification notification = Notification.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .post(post)
                .comment(comment)
                .type(type)
                .read(false)
                .build();
        
        Notification savedNotification = notificationRepository.save(notification);
        notifyUser(toUser.getId(), savedNotification); // Send SSE notification
    }
    
    @Transactional
    public void createReviewNotification(User fromUser, User toUser, Review review, ReviewComment reviewComment, Notification.NotificationType type) {
        if (fromUser.getId().equals(toUser.getId())) return;
        
        Notification notification = new Notification();
        notification.setFromUser(fromUser);
        notification.setToUser(toUser);
        notification.setReview(review);
        notification.setReviewComment(reviewComment);
        notification.setType(type);
        notification.setRead(false);
        
        Notification savedNotification = notificationRepository.save(notification);
        notifyUser(toUser.getId(), savedNotification); // Send SSE notification
    }
    
    @Transactional
    public void createFollowNotification(User fromUser, User toUser) {
        if (fromUser.getId().equals(toUser.getId())) return;
        
        Notification notification = new Notification();
        notification.setFromUser(fromUser);
        notification.setToUser(toUser);
        notification.setType(Notification.NotificationType.FOLLOW);
        notification.setRead(false);

        Notification savedNotification = notificationRepository.save(notification);
        notifyUser(toUser.getId(), savedNotification); // Send SSE notification
    }
    
    @Transactional
    public void deleteAllNotifications(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        notificationRepository.deleteByToUser(user);
        log.info("User {}'s all notifications deleted.", userId);
    }

    /**
     * Registers an SseEmitter for a given user.
     * @param userId The ID of the user.
     * @param emitter The SseEmitter instance.
     */
    public void registerEmitter(Long userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        log.info("Emitter registered for user: {}", userId);

        // Remove emitter on completion, timeout, or error
        emitter.onCompletion(() -> {
            log.info("Emitter completed for user: {}", userId);
            emitters.remove(userId);
        });
        emitter.onTimeout(() -> {
            log.info("Emitter timed out for user: {}", userId);
            emitters.remove(userId);
        });
        emitter.onError((e) -> {
            log.error("Emitter error for user: {}: {}", userId, e.getMessage());
            emitters.remove(userId);
        });
    }

    /**
     * Sends a notification event to a specific user via SSE.
     * This method should be called after a notification is saved.
     * Uses @Async to avoid blocking the main thread.
     * @param userId The ID of the user to notify.
     * @param notification The Notification entity that was saved.
     */
    @Async
    public void notifyUser(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                NotificationResponse response = convertToResponse(notification); // Convert to DTO
                emitter.send(SseEmitter.event()
                        .name("notification") // Event name expected by frontend
                        .data(response, MediaType.APPLICATION_JSON));
                log.info("Sent notification to user {}: Type={}, ID={}", 
                         userId, notification.getType(), notification.getId());
            } catch (IOException e) {
                log.error("Failed to send SSE notification to user {}: {}", userId, e.getMessage());
                emitters.remove(userId); // Remove emitter on failure
            } catch (Exception e) {
                log.error("Unexpected error sending SSE notification to user {}: {}", userId, e.getMessage(), e);
                emitters.remove(userId); // Remove emitter on failure
            }
        } else {
            log.warn("No active SSE emitter found for user {}", userId);
        }
    }
    
    // Entity를 Response DTO로 변환하는 메서드
    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setId(notification.getId());
        response.setType(notification.getType().name());
        response.setCreatedAt(notification.getCreatedAt());
        response.setRead(notification.isRead());
        
        // 알림을 보낸 사용자 정보
        response.setFromUser(new NotificationResponse.UserSummary(
                notification.getFromUser().getId(),
                notification.getFromUser().getUsername(),
                notification.getFromUser().getProfileImageUrl()
        ));
        
        // 게시물 정보
        if (notification.getPost() != null) {
            response.setPostId(notification.getPost().getId());
            response.setPostTitle(notification.getPost().getTitle());
        }
        
        // 리뷰 정보
        if (notification.getReview() != null) {
            response.setReviewId(notification.getReview().getId());
            response.setReviewTitle(notification.getReview().getTitle());
            response.setMovieId(notification.getReview().getMovieId());
            response.setMovieTitle(notification.getReview().getMovieTitle());
        }
        
        // 댓글 정보 (Post 또는 Review 댓글 중 하나만 존재)
        if (notification.getComment() != null) {
            response.setCommentId(notification.getComment().getId());
            response.setCommentContent(notification.getComment().getContent());
        } else if (notification.getReviewComment() != null) {
            response.setCommentId(notification.getReviewComment().getId());
            response.setCommentContent(notification.getReviewComment().getContent());
        }
        
        return response;
    }
} 