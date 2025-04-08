package com.moviesocial.service;

import com.moviesocial.model.*;
import com.moviesocial.model.ERole;
import com.moviesocial.payload.request.CommentRequest;
import com.moviesocial.payload.response.CommentResponse;
import com.moviesocial.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.Hibernate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.stream.Collectors;

public interface CommentService {
    
    List<CommentResponse> getCommentsByPostId(Long postId, Long currentUserId);
    
    CommentResponse createComment(Long postId, CommentRequest commentRequest, Long userId);
    
    CommentResponse updateComment(Long commentId, CommentRequest commentRequest, Long userId);
    
    void deleteComment(Long commentId, Long userId);
    
    CommentResponse likeComment(Long commentId, Long userId);
    
    CommentResponse dislikeComment(Long commentId, Long userId);
    
    Page<Comment> getComments(Long postId, Pageable pageable);
} 