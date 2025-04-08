package com.moviesocial.service.impl;

import com.moviesocial.exception.ResourceNotFoundException;
import com.moviesocial.model.User;
import com.moviesocial.repository.UserRepository;
import com.moviesocial.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    @Override
    public Page<User> searchUsersByUsername(String username, Pageable pageable) {
        if (username == null || username.trim().isEmpty()) {
            return Page.empty(pageable);
        }
        return userRepository.findByUsernameContaining(username.trim(), pageable);
    }
} 