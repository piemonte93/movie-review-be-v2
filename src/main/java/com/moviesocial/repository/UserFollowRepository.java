package com.moviesocial.repository;

import com.moviesocial.model.User;
import com.moviesocial.model.UserFollow;
import com.moviesocial.model.UserFollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, UserFollowId> {

    /**
     * 특정 사용자의 팔로워 목록 조회
     */
    @Query("SELECT uf.follower FROM UserFollow uf WHERE uf.following.id = :userId")
    List<User> findFollowersByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 팔로우하는 사용자 목록 조회
     */
    @Query("SELECT uf.following FROM UserFollow uf WHERE uf.follower.id = :userId")
    List<User> findFollowingByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 팔로워 수 조회 - 네이티브 쿼리 사용
     */
    @Query(value = "SELECT COUNT(*) FROM user_following WHERE following_id = :userId", nativeQuery = true)
    long countByFollowingId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 팔로우하는 사용자 수 조회 - 네이티브 쿼리 사용
     */
    @Query(value = "SELECT COUNT(*) FROM user_following WHERE follower_id = :userId", nativeQuery = true)
    long countByFollowerId(@Param("userId") Long userId);

    /**
     * 두 사용자 간의 팔로우 관계 조회
     */
    default Optional<UserFollow> findByFollowerIdAndFollowingId(Long followerId, Long followingId) {
        return findById(new UserFollowId(followerId, followingId));
    }

    /**
     * 특정 사용자의 팔로우 상태와 함께 팔로워 목록 조회
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id IN " +
           "(SELECT uf.follower.id FROM UserFollow uf WHERE uf.following.id = :userId)")
    List<User> findFollowersWithRolesByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 팔로우 상태와 함께 팔로잉 목록 조회
     */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.id IN " +
           "(SELECT uf.following.id FROM UserFollow uf WHERE uf.follower.id = :userId)")
    List<User> findFollowingWithRolesByUserId(@Param("userId") Long userId);
} 