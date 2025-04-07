package com.musicstore.repository;

import com.musicstore.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, Long> {
    List<Artist> findByNameContainingIgnoreCase(String name);
    List<Artist> findByCountryIgnoreCase(String country);
    List<Artist> findByNameContainingIgnoreCaseOrCountryContainingIgnoreCase(String name, String country);
}