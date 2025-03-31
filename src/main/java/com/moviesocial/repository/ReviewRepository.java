package com.moviesocial.repository;

import com.moviesocial.model.Review;
import com.moviesocial.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<Review> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(r.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Review> searchByKeyword(String keyword, Pageable pageable);
    
    List<Review> findTop5ByUserOrderByCreatedAtDesc(User user);
    
    Optional<Review> findByIdAndUser(Long id, User user);
    
    Page<Review> findByMovieId(Long movieId, Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM Review r WHERE r.user = :user")
    Long countByUser(User user);
    
    boolean existsByUserAndMovieId(User user, Long movieId);

    @Query(value = """
           SELECT DISTINCT r FROM Review r 
           LEFT JOIN FETCH r.user 
           LEFT JOIN FETCH r.likes 
           LEFT JOIN FETCH r.dislikes 
           LEFT JOIN FETCH r.comments c 
           LEFT JOIN FETCH c.user 
           WHERE r.id IN :ids 
           ORDER BY r.createdAt DESC""")
    List<Review> findAllWithDetailsById(@Param("ids") List<Long> ids);

    @Query(value = """
           SELECT r FROM Review r 
           LEFT JOIN FETCH r.user 
           ORDER BY r.createdAt DESC""",
           countQuery = "SELECT COUNT(r) FROM Review r")
    Page<Review> findAllWithUser(Pageable pageable);

    default Page<Review> findAllWithDetails(Pageable pageable) {
        Page<Review> reviews = findAllWithUser(pageable);
        if (!reviews.hasContent()) {
            return reviews;
        }
        
        List<Long> reviewIds = reviews.getContent().stream()
                .map(Review::getId)
                .toList();
        
        List<Review> reviewsWithDetails = findAllWithDetailsById(reviewIds);
        
        return new PageImpl<>(reviewsWithDetails, pageable, reviews.getTotalElements());
    }
} 