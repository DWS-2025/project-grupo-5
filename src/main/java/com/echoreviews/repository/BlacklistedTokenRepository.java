package com.echoreviews.repository;

import com.echoreviews.model.BlacklistedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, String> {
    
    Optional<BlacklistedToken> findByTokenId(String tokenId);
    
    boolean existsByTokenId(String tokenId);
    
    @Modifying
    @Query("DELETE FROM BlacklistedToken b WHERE b.expirationDate < :now")
    void deleteAllExpiredTokens(@Param("now") Date now);
} 