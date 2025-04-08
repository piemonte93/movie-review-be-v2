package com.moviesocial.service;

import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {
    
    User getUserById(Long userId);

    User getUserByUsername(String username);
    
    Page<User> searchUsersByUsername(String username, Pageable pageable);
} 