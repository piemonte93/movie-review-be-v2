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

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;
    
    @Autowired
    private UserRepository userRepository;
    
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
        // 자신에게 보내는 알림은 생성하지 않음
        if (fromUser.getId().equals(toUser.getId())) {
            return;
        }
        
        Notification notification = Notification.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .post(post)
                .comment(comment)
                .type(type)
                .read(false)
                .build();
        
        notificationRepository.save(notification);
    }
    
    // 리뷰 관련 알림 생성
    @Transactional
    public void createReviewNotification(User fromUser, User toUser, Review review, Comment comment, Notification.NotificationType type) {
        // 자신에게 보내는 알림은 생성하지 않음
        if (fromUser.getId().equals(toUser.getId())) {
            return;
        }
        
        Notification notification = Notification.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .review(review)
                .comment(comment)
                .type(type)
                .read(false)
                .build();
        
        notificationRepository.save(notification);
    }
    
    // 팔로우 알림 생성
    @Transactional
    public void createFollowNotification(User fromUser, User toUser) {
        // 자신에게 보내는 알림은 생성하지 않음
        if (fromUser.getId().equals(toUser.getId())) {
            return;
        }
        
        Notification notification = Notification.builder()
                .fromUser(fromUser)
                .toUser(toUser)
                .type(Notification.NotificationType.FOLLOW)
                .read(false)
                .build();
        
        notificationRepository.save(notification);
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
        
        // 댓글 정보
        if (notification.getComment() != null) {
            response.setCommentId(notification.getComment().getId());
            response.setCommentContent(notification.getComment().getContent());
        }
        
        return response;
    }
} 