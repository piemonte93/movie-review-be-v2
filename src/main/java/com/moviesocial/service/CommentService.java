package com.moviesocial.service;

import com.moviesocial.model.*;
import com.moviesocial.payload.request.CommentRequest;
import com.moviesocial.payload.response.CommentResponse;
import com.moviesocial.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    
    @Autowired
    private CommentDislikeRepository commentDislikeRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    public List<CommentResponse> getCommentsByPostId(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + postId));
        
        List<Comment> comments = commentRepository.findByPostOrderByCreatedAtAsc(post);
        
        return comments.stream()
                .map(comment -> convertToResponse(comment, currentUserId))
                .collect(Collectors.toList());
    }
    
    @Transactional
    public CommentResponse createComment(Long postId, CommentRequest commentRequest, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + postId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        Comment comment = Comment.builder()
                .content(commentRequest.getContent())
                .post(post)
                .user(user)
                .build();
        
        Comment savedComment = commentRepository.save(comment);
        
        // 게시물 작성자와 댓글 작성자가 다른 경우에만 알림 생성
        if (!post.getUser().getId().equals(userId)) {
            notificationService.createNotification(
                    user,
                    post.getUser(),
                    post,
                    savedComment,
                    Notification.NotificationType.COMMENT
            );
        }
        
        return convertToResponse(savedComment, userId);
    }
    
    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest commentRequest, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("이 댓글을 수정할 권한이 없습니다.");
        }
        
        comment.setContent(commentRequest.getContent());
        Comment updatedComment = commentRepository.save(comment);
        
        return convertToResponse(updatedComment, userId);
    }
    
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("이 댓글을 삭제할 권한이 없습니다.");
        }
        
        commentRepository.delete(comment);
    }
    
    @Transactional
    public CommentResponse likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 이미 좋아요한 경우 좋아요 취소
        if (commentLikeRepository.existsByCommentAndUser(comment, user)) {
            commentLikeRepository.deleteByCommentAndUser(comment, user);
        } else {
            // 싫어요가 있으면 제거
            if (commentDislikeRepository.existsByCommentAndUser(comment, user)) {
                commentDislikeRepository.deleteByCommentAndUser(comment, user);
            }
            
            // 좋아요 추가
            CommentLike like = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            
            commentLikeRepository.save(like);
            
            // 본인 댓글이 아닌 경우에만 알림 생성
            if (!comment.getUser().getId().equals(userId)) {
                notificationService.createNotification(
                        user,
                        comment.getUser(),
                        comment.getPost(),
                        comment,
                        Notification.NotificationType.LIKE
                );
            }
        }
        
        return convertToResponse(comment, userId);
    }
    
    @Transactional
    public CommentResponse dislikeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 이미 싫어요한 경우 싫어요 취소
        if (commentDislikeRepository.existsByCommentAndUser(comment, user)) {
            commentDislikeRepository.deleteByCommentAndUser(comment, user);
        } else {
            // 좋아요가 있으면 제거
            if (commentLikeRepository.existsByCommentAndUser(comment, user)) {
                commentLikeRepository.deleteByCommentAndUser(comment, user);
            }
            
            // 싫어요 추가
            CommentDislike dislike = CommentDislike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            
            commentDislikeRepository.save(dislike);
        }
        
        return convertToResponse(comment, userId);
    }
    
    // Entity를 Response DTO로 변환하는 메서드
    private CommentResponse convertToResponse(Comment comment, Long currentUserId) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt());
        response.setPostId(comment.getPost().getId());
        
        // 사용자 정보
        response.setUser(new CommentResponse.UserSummary(
                comment.getUser().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getProfileImageUrl()
        ));
        
        // 좋아요, 싫어요 카운트
        response.setLikeCount(comment.getLikeCount());
        response.setDislikeCount(comment.getDislikeCount());
        
        // 현재 사용자가 좋아요/싫어요 했는지 여부
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                response.setLiked(commentLikeRepository.existsByCommentAndUser(comment, currentUser));
                response.setDisliked(commentDislikeRepository.existsByCommentAndUser(comment, currentUser));
            }
        }
        
        return response;
    }
} 