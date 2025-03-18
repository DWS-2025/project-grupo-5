package com.musicstore.repository;

import com.musicstore.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByTitleContainingIgnoreCase(String title);
    List<Album> findByGenreIgnoreCase(String genre);
    List<Album> findByArtistNameContainingIgnoreCase(String artistName);
    List<Album> findByYearOrderByTitleAsc(Integer year);
    
    @Query("SELECT a FROM Album a WHERE a.averageRating >= :rating ORDER BY a.averageRating DESC")
    List<Album> findByAverageRatingGreaterThanEqual(@Param("rating") Double rating);
}