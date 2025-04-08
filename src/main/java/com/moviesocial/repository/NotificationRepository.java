package com.moviesocial.repository;

import com.moviesocial.model.Notification;
import com.moviesocial.model.User;
import com.moviesocial.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByToUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    long countByToUserAndReadFalse(User user);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.toUser.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.comment.id = :commentId")
    void deleteByCommentId(@Param("commentId") Long commentId);
    
    // 특정 댓글에 관련된 알림 목록 찾기
    List<Notification> findByCommentId(Long commentId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.toUser = :user AND n.read = false")
    void markAllAsReadByUser(@Param("user") User user);

    // Method to delete all notifications for a specific user
    void deleteByToUser(User toUser);
} 