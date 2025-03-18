package com.musicstore.repository;

import com.musicstore.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserId(Long userId);
    List<Review> findByAlbumId(Long albumId);
    List<Review> findByUserIdAndAlbumId(Long userId, Long albumId);
    List<Review> findByRatingGreaterThanEqual(Integer rating);
}