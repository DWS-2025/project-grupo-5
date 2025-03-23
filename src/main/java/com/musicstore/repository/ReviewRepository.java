package com.musicstore.repository;

import com.musicstore.model.Review;
import com.musicstore.model.User;
import com.musicstore.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUser(User user);
    List<Review> findByAlbum(Album album);
    List<Review> findByUserAndAlbum(User user, Album album);
    List<Review> findByRatingGreaterThanEqual(Integer rating);
}