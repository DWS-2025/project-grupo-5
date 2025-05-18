package com.echoreviews.service;

import com.echoreviews.model.BlacklistedToken;
import com.echoreviews.repository.BlacklistedTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenBlacklistService {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    /**
     * Add a token to the blacklist
     */
    public void blacklistToken(String tokenId, Date expirationDate, String username) {
        BlacklistedToken blacklistedToken = new BlacklistedToken(
                tokenId,
                expirationDate,
                new Date(),
                username
        );
        blacklistedTokenRepository.save(blacklistedToken);
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String tokenId) {
        return blacklistedTokenRepository.existsByTokenId(tokenId);
    }

    /**
     * Scheduled task to remove expired tokens from the blacklist
     */
    @Scheduled(cron = "0 0 * * * *") // Run every hour
    @Transactional
    public void cleanupExpiredTokens() {
        blacklistedTokenRepository.deleteAllExpiredTokens(new Date());
    }
} 