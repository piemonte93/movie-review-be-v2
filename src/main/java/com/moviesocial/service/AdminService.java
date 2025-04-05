package com.moviesocial.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moviesocial.model.User;
import com.moviesocial.repository.UserRepository;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;
    
    /**
     * 전체 사용자 목록 조회
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }
    
    /**
     * 사용자 상태 업데이트
     */
    @Transactional
    public User updateUserStatus(Long userId, User.UserStatus status, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
        
        user.setStatus(status);
        
        if (status == User.UserStatus.BLOCKED) {
            user.setBlockReason(reason);
        } else {
            user.setBlockReason(null);
            user.setBlockDate(null);
        }
        
        return userRepository.save(user);
    }
} 