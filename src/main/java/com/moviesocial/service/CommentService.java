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
    
    @Autowired
    private NotificationRepository notificationRepository;
    
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
        try {
            System.out.println("댓글 삭제 서비스 시작 - commentId: " + commentId + ", userId: " + userId);
            
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
            
            System.out.println("댓글 조회 성공 - commentId: " + commentId + ", 작성자 ID: " + comment.getUser().getId());
            
            // 자신의 댓글이거나 관리자/모더레이터인 경우 삭제 가능
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다. ID: " + userId));
            
            boolean isAdminOrModerator = user.getRoles().stream()
                    .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN || role.getName() == ERole.ROLE_MODERATOR);
            
            if (!comment.getUser().getId().equals(userId) && !isAdminOrModerator) {
                System.out.println("권한 오류 - 요청 사용자 ID: " + userId + ", 댓글 작성자 ID: " + comment.getUser().getId());
                throw new RuntimeException("이 댓글을 삭제할 권한이 없습니다.");
            }
            
            // 1. 댓글과 관련된 알림 삭제
            System.out.println("댓글 관련 알림 삭제 시작");
            notificationRepository.deleteByCommentId(commentId);
            System.out.println("댓글 관련 알림 삭제 완료");
            
            // 2. 댓글의 좋아요와 싫어요를 먼저 수동으로 삭제 (참조 무결성을 위해)
            for (CommentLike like : comment.getLikes()) {
                commentLikeRepository.delete(like);
            }
            
            for (CommentDislike dislike : comment.getDislikes()) {
                commentDislikeRepository.delete(dislike);
            }
            
            System.out.println("댓글의 좋아요/싫어요 삭제 완료");
            
            // 3. 댓글 삭제
            commentRepository.delete(comment);
            
            System.out.println("댓글 삭제 완료 - commentId: " + commentId);
        } catch (Exception e) {
            System.out.println("댓글 삭제 서비스에서 예외 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Transactional
    public CommentResponse likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 초기 상태 로깅
        System.out.println("댓글 좋아요 처리 전 상태 확인");
        System.out.println("Comment ID: " + comment.getId());
        System.out.println("댓글 좋아요 처리 전 likes 컬렉션 상태: " + (comment.getLikes() != null ? "not null" : "null"));
        System.out.println("댓글 좋아요 처리 전 dislikes 컬렉션 상태: " + (comment.getDislikes() != null ? "not null" : "null"));
        
        boolean wasLiked = commentLikeRepository.existsByCommentAndUser(comment, user);
        boolean wasDisliked = commentDislikeRepository.existsByCommentAndUser(comment, user);
        
        System.out.println("댓글 좋아요 처리 전 상태 - 좋아요 상태: " + wasLiked + ", 싫어요 상태: " + wasDisliked);
        
        // 이미 좋아요한 경우 좋아요 취소
        if (wasLiked) {
            System.out.println("이미 댓글에 좋아요 상태 -> 좋아요 취소");
            commentLikeRepository.deleteByCommentAndUser(comment, user);
        } else {
            // 싫어요가 있으면 제거
            if (wasDisliked) {
                System.out.println("댓글에 싫어요 상태에서 좋아요로 변경 -> 싫어요 취소");
                commentDislikeRepository.deleteByCommentAndUser(comment, user);
            }
            
            // 좋아요 추가
            System.out.println("댓글에 좋아요 추가");
            CommentLike like = CommentLike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            
            commentLikeRepository.save(like);
        }
        
        // 명시적으로 저장하여 변경 사항을 데이터베이스에 반영
        comment = commentRepository.save(comment);
        commentRepository.flush();
        
        // 영속성 컨텍스트 초기화 후 엔티티 다시 로드
        comment = commentRepository.findById(commentId).orElseThrow(() -> 
            new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        // 컬렉션 초기화
        Hibernate.initialize(comment.getLikes());
        Hibernate.initialize(comment.getDislikes());
        
        System.out.println("댓글 좋아요 처리 후 상태 확인");
        System.out.println("댓글 좋아요 컬렉션 초기화 상태: " + Hibernate.isInitialized(comment.getLikes()));
        System.out.println("댓글 싫어요 컬렉션 초기화 상태: " + Hibernate.isInitialized(comment.getDislikes()));
        System.out.println("댓글 좋아요 컬렉션 실제 크기: " + comment.getLikes().size());
        System.out.println("댓글 싫어요 컬렉션 실제 크기: " + comment.getDislikes().size());
        System.out.println("댓글의 좋아요 수(getLikeCount): " + comment.getLikeCount());
        System.out.println("댓글의 싫어요 수(getDislikeCount): " + comment.getDislikeCount());
        
        // 현재 좋아요/싫어요 상태 확인
        boolean isLikedNow = commentLikeRepository.existsByCommentAndUser(comment, user);
        boolean isDislikedNow = commentDislikeRepository.existsByCommentAndUser(comment, user);
        System.out.println("댓글 처리 후 상태 - 좋아요: " + isLikedNow + ", 싫어요: " + isDislikedNow);
        
        CommentResponse response = convertToResponse(comment, userId);
        System.out.println("CommentResponse 생성 후 - 좋아요 수: " + response.getLikeCount());
        System.out.println("CommentResponse 생성 후 - 싫어요 수: " + response.getDislikeCount());
        System.out.println("CommentResponse 생성 후 - 좋아요 상태: " + response.isLiked());
        System.out.println("CommentResponse 생성 후 - 싫어요 상태: " + response.isDisliked());
        
        return response;
    }
    
    @Transactional
    public CommentResponse dislikeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 초기 상태 로깅
        System.out.println("댓글 싫어요 처리 전 상태 확인");
        System.out.println("Comment ID: " + comment.getId());
        System.out.println("댓글 싫어요 처리 전 likes 컬렉션 상태: " + (comment.getLikes() != null ? "not null" : "null"));
        System.out.println("댓글 싫어요 처리 전 dislikes 컬렉션 상태: " + (comment.getDislikes() != null ? "not null" : "null"));
        
        boolean wasLiked = commentLikeRepository.existsByCommentAndUser(comment, user);
        boolean wasDisliked = commentDislikeRepository.existsByCommentAndUser(comment, user);
        
        System.out.println("댓글 싫어요 처리 전 상태 - 좋아요 상태: " + wasLiked + ", 싫어요 상태: " + wasDisliked);
        
        // 이미 싫어요한 경우 싫어요 취소
        if (wasDisliked) {
            System.out.println("이미 댓글에 싫어요 상태 -> 싫어요 취소");
            commentDislikeRepository.deleteByCommentAndUser(comment, user);
        } else {
            // 좋아요가 있으면 제거
            if (wasLiked) {
                System.out.println("댓글에 좋아요 상태에서 싫어요로 변경 -> 좋아요 취소");
                commentLikeRepository.deleteByCommentAndUser(comment, user);
            }
            
            // 싫어요 추가
            System.out.println("댓글에 싫어요 추가");
            CommentDislike dislike = CommentDislike.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            
            commentDislikeRepository.save(dislike);
        }
        
        // 명시적으로 저장하여 변경 사항을 데이터베이스에 반영
        comment = commentRepository.save(comment);
        commentRepository.flush();
        
        // 영속성 컨텍스트 초기화 후 엔티티 다시 로드
        comment = commentRepository.findById(commentId).orElseThrow(() -> 
            new RuntimeException("댓글을 찾을 수 없습니다. ID: " + commentId));
        
        // 컬렉션 초기화
        Hibernate.initialize(comment.getLikes());
        Hibernate.initialize(comment.getDislikes());
        
        System.out.println("댓글 싫어요 처리 후 상태 확인");
        System.out.println("댓글 좋아요 컬렉션 초기화 상태: " + Hibernate.isInitialized(comment.getLikes()));
        System.out.println("댓글 싫어요 컬렉션 초기화 상태: " + Hibernate.isInitialized(comment.getDislikes()));
        System.out.println("댓글 좋아요 컬렉션 실제 크기: " + comment.getLikes().size());
        System.out.println("댓글 싫어요 컬렉션 실제 크기: " + comment.getDislikes().size());
        System.out.println("댓글의 좋아요 수(getLikeCount): " + comment.getLikeCount());
        System.out.println("댓글의 싫어요 수(getDislikeCount): " + comment.getDislikeCount());
        
        // 현재 좋아요/싫어요 상태 확인
        boolean isLikedNow = commentLikeRepository.existsByCommentAndUser(comment, user);
        boolean isDislikedNow = commentDislikeRepository.existsByCommentAndUser(comment, user);
        System.out.println("댓글 처리 후 상태 - 좋아요: " + isLikedNow + ", 싫어요: " + isDislikedNow);
        
        CommentResponse response = convertToResponse(comment, userId);
        System.out.println("CommentResponse 생성 후 - 좋아요 수: " + response.getLikeCount());
        System.out.println("CommentResponse 생성 후 - 싫어요 수: " + response.getDislikeCount());
        System.out.println("CommentResponse 생성 후 - 좋아요 상태: " + response.isLiked());
        System.out.println("CommentResponse 생성 후 - 싫어요 상태: " + response.isDisliked());
        
        return response;
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