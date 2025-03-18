package com.musicstore.repository;

import com.musicstore.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndPassword(String username, String password);
    
    @Query("SELECT u.username FROM User u JOIN u.favoriteAlbums a WHERE a.id = :albumId")
    List<String> findUsernamesByFavoriteAlbumId(@Param("albumId") Long albumId);
    
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}