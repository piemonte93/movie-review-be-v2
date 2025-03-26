package com.moviesocial.repository;

import com.moviesocial.model.Notification;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByToUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    long countByToUserAndReadFalse(User user);
    
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.toUser.id = :userId")
    void markAllAsRead(@Param("userId") Long userId);
} 