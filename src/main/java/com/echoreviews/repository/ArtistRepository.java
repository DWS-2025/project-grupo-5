package com.echoreviews.repository;

import com.echoreviews.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {

    @Query("SELECT a FROM Artist a LEFT JOIN FETCH a.albums WHERE a.id = :id")
    Optional<Artist> findByIdWithAlbums(@Param("id") Long id);
    List<Artist> findByNameContainingIgnoreCase(String name);
    List<Artist> findByCountryIgnoreCase(String country);
    List<Artist> findByNameContainingIgnoreCaseOrCountryContainingIgnoreCase(String name, String country);
}