package com.musicstore.repository;

import com.musicstore.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    Page<Album> findAll(Pageable pageable);

    List<Album> findByTitleContainingIgnoreCase(String title);
    List<Album> findByGenreIgnoreCase(String genre);
    @Query("SELECT DISTINCT a FROM Album a JOIN a.artists art WHERE LOWER(art.name) LIKE LOWER(CONCAT('%', :artistName, '%'))")
    List<Album> findByArtistNameContainingIgnoreCase(@Param("artistName") String artistName);
    List<Album> findByYearOrderByTitleAsc(Integer year);
    
    @Query("SELECT a FROM Album a WHERE a.averageRating >= :rating ORDER BY a.averageRating DESC")
    List<Album> findByAverageRatingGreaterThanEqual(@Param("rating") Double rating);
}