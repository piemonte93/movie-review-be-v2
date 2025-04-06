package com.moviesocial.service;

import com.moviesocial.model.*;
import com.moviesocial.payload.request.PostRequest;
import com.moviesocial.payload.response.PostResponse;
import com.moviesocial.payload.response.CommentResponse;
import com.moviesocial.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PostService {

    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private CommentRepository commentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostLikeRepository postLikeRepository;
    
    @Autowired
    private PostDislikeRepository postDislikeRepository;
    
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    
    @Autowired
    private CommentDislikeRepository commentDislikeRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    public Page<PostResponse> getAllPosts(Pageable pageable, Long currentUserId) {
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        System.out.println("Found " + posts.getTotalElements() + " posts");
        System.out.println("Current page: " + posts.getNumber());
        System.out.println("Total pages: " + posts.getTotalPages());
        
        return posts.map(post -> {
            PostResponse response = convertToResponse(post, currentUserId);
            System.out.println("Converting post ID: " + post.getId() + " to response");
            return response;
        });
    }
    
    public PostResponse getPostById(Long id, Long currentUserId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + id));
        return convertToResponse(post, currentUserId);
    }
    
    @Transactional
    public PostResponse createPost(PostRequest postRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        Post post = Post.builder()
                .title(postRequest.getTitle())
                .content(postRequest.getContent())
                .user(user)
                .build();
        
        // 멘션 추출 및 저장
        Set<User> mentionedUsers = extractMentions(postRequest.getContent());
        post.setMentions(mentionedUsers);
        
        Post savedPost = postRepository.save(post);
        
        // 멘션된 사용자에게 알림 생성
        for (User mentionedUser : mentionedUsers) {
            if (!mentionedUser.getId().equals(userId)) {
                notificationService.createNotification(
                        user,
                        mentionedUser,
                        savedPost,
                        null,
                        Notification.NotificationType.MENTION
                );
            }
        }
        
        return convertToResponse(savedPost, userId);
    }
    
    @Transactional
    public PostResponse updatePost(Long id, PostRequest postRequest, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + id));
        
        if (!post.getUser().getId().equals(userId)) {
            throw new RuntimeException("이 게시물을 수정할 권한이 없습니다.");
        }
        
        // 이전 멘션 목록 저장
        Set<User> oldMentions = new HashSet<>(post.getMentions());
        
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        
        // 새로운 멘션 추출 및 저장
        Set<User> newMentions = extractMentions(postRequest.getContent());
        post.setMentions(newMentions);
        
        Post updatedPost = postRepository.save(post);
        
        // 새로 멘션된 사용자에게만 알림 생성
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
                
        newMentions.stream()
                .filter(u -> !oldMentions.contains(u) && !u.getId().equals(userId))
                .forEach(mentionedUser -> {
                    notificationService.createNotification(
                            currentUser,
                            mentionedUser,
                            updatedPost,
                            null,
                            Notification.NotificationType.MENTION
                    );
                });
        
        return convertToResponse(updatedPost, userId);
    }
    
    @Transactional
    public void deletePost(Long id, Long userId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + id));
        
        // 자신의 게시물이거나 관리자/모더레이터인 경우 삭제 허용
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        boolean isAuthor = post.getUser().getId().equals(userId);
        boolean isAdminOrModerator = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName() == ERole.ROLE_ADMIN || role.getName() == ERole.ROLE_MODERATOR);
                
        if (!isAuthor && !isAdminOrModerator) {
            throw new RuntimeException("이 게시물을 삭제할 권한이 없습니다.");
        }
        
        postRepository.delete(post);
    }
    
    @Transactional
    public PostResponse likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + postId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 이미 좋아요한 경우 좋아요 취소
        if (postLikeRepository.existsByPostAndUser(post, user)) {
            postLikeRepository.deleteByPostAndUser(post, user);
        } else {
            // 싫어요가 있으면 제거
            if (postDislikeRepository.existsByPostAndUser(post, user)) {
                postDislikeRepository.deleteByPostAndUser(post, user);
            }
            
            // 좋아요 추가
            PostLike like = PostLike.builder()
                    .post(post)
                    .user(user)
                    .build();
            
            postLikeRepository.save(like);
            
            // 자신의 게시물이 아닌 경우에만 알림 생성
            if (!post.getUser().getId().equals(userId)) {
                notificationService.createNotification(
                        user,
                        post.getUser(),
                        post,
                        null,
                        Notification.NotificationType.LIKE
                );
            }
        }
        
        return convertToResponse(post, userId);
    }
    
    @Transactional
    public PostResponse dislikePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("게시물을 찾을 수 없습니다. ID: " + postId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        // 이미 싫어요한 경우 싫어요 취소
        if (postDislikeRepository.existsByPostAndUser(post, user)) {
            postDislikeRepository.deleteByPostAndUser(post, user);
        } else {
            // 좋아요가 있으면 제거
            if (postLikeRepository.existsByPostAndUser(post, user)) {
                postLikeRepository.deleteByPostAndUser(post, user);
            }
            
            // 싫어요 추가
            PostDislike dislike = PostDislike.builder()
                    .post(post)
                    .user(user)
                    .build();
            
            postDislikeRepository.save(dislike);
        }
        
        return convertToResponse(post, userId);
    }
    
    public Page<PostResponse> searchPosts(String query, String category, Pageable pageable, Long currentUserId) {
        Page<Post> results;
        
        switch (category) {
            case "title":
                results = postRepository.searchByTitle(query, pageable);
                break;
            case "content":
                results = postRepository.searchByContent(query, pageable);
                break;
            case "author":
                results = postRepository.searchByAuthor(query, pageable);
                break;
            default:
                results = postRepository.searchByTitle(query, pageable);
                break;
        }
        
        return results.map(post -> convertToResponse(post, currentUserId));
    }
    
    public Page<PostResponse> getUserPosts(Long userId, Pageable pageable, Long currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
        
        Page<Post> posts = postRepository.findByUser(user, pageable);
        return posts.map(post -> convertToResponse(post, currentUserId));
    }
    
    // 멘션된 사용자 추출 메서드
    private Set<User> extractMentions(String content) {
        Set<User> mentionedUsers = new HashSet<>();
        Pattern pattern = Pattern.compile("@([\\w가-힣]+)");
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String username = matcher.group(1);
            userRepository.findByUsername(username).ifPresent(mentionedUsers::add);
        }
        
        return mentionedUsers;
    }
    
    // Entity를 Response DTO로 변환하는 메서드
    private PostResponse convertToResponse(Post post, Long currentUserId) {
        System.out.println("Converting post to response - ID: " + post.getId());
        System.out.println("Title: " + post.getTitle());
        System.out.println("Content length: " + post.getContent().length());
        System.out.println("Created at: " + post.getCreatedAt());
        System.out.println("User ID: " + post.getUser().getId());
        
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setCreatedAt(post.getCreatedAt());
        response.setPostId(post.getId());
        
        // 사용자 정보
        response.setUser(new PostResponse.UserSummary(
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getProfileImageUrl()
        ));
        
        // 좋아요, 싫어요 카운트
        response.setLikeCount(post.getLikeCount());
        response.setDislikeCount(post.getDislikeCount());
        response.setCommentCount(post.getCommentCount());
        
        // 현재 사용자가 좋아요/싫어요 했는지 여부
        if (currentUserId != null) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                response.setLiked(postLikeRepository.existsByPostAndUser(post, currentUser));
                response.setDisliked(postDislikeRepository.existsByPostAndUser(post, currentUser));
            }
        }
        
        // 멘션된 사용자 정보
        response.setMentions(post.getMentions().stream()
                .map(user -> new PostResponse.UserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getProfileImageUrl()
                ))
                .collect(Collectors.toSet()));
        
        // 댓글 정보
        response.setComments(post.getComments().stream()
                .map(comment -> {
                    CommentResponse commentResponse = new CommentResponse();
                    commentResponse.setId(comment.getId());
                    commentResponse.setContent(comment.getContent());
                    commentResponse.setCreatedAt(comment.getCreatedAt());
                    commentResponse.setPostId(comment.getPost().getId());
                    commentResponse.setUser(new CommentResponse.UserSummary(
                            comment.getUser().getId(),
                            comment.getUser().getUsername(),
                            comment.getUser().getProfileImageUrl()
                    ));
                    commentResponse.setLikeCount(comment.getLikeCount());
                    commentResponse.setDislikeCount(comment.getDislikeCount());
                    
                    if (currentUserId != null) {
                        User currentUser = userRepository.findById(currentUserId).orElse(null);
                        if (currentUser != null) {
                            commentResponse.setLiked(commentLikeRepository.existsByCommentAndUser(comment, currentUser));
                            commentResponse.setDisliked(commentDislikeRepository.existsByCommentAndUser(comment, currentUser));
                        }
                    }
                    
                    return commentResponse;
                })
                .collect(Collectors.toList()));
        
        System.out.println("Successfully converted post to response");
        return response;
    }
} 