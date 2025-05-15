package com.echoreviews.repository;

import com.echoreviews.model.Review;
import com.echoreviews.model.User;
import com.echoreviews.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUser(User user);
    List<Review> findByAlbum(Album album);
    List<Review> findByUserAndAlbum(User user, Album album);
    List<Review> findByRatingGreaterThanEqual(Integer rating);
    List<Review> findByAlbum_Id(Long albumId);
    @Query("SELECT r FROM Review r JOIN FETCH r.album WHERE r.user.id = :userId") List<Review> findByUser_Id(@Param("userId") Long userId);
}