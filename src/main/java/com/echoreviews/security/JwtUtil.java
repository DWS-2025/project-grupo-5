package com.echoreviews.security;

import com.echoreviews.service.TokenBlacklistService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("234dSecreT_MeeT1ng!256@Si?LeesEstoMe_Deb3s_3_eur0sySiSiguesLeyendoMe_Debes10")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;
    
    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    private Key getSigningKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenId(String token) {
        return extractClaim(token, claims -> claims.get("tokenId", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Boolean isTokenBlacklisted(String token) {
        String tokenId = extractTokenId(token);
        return tokenBlacklistService.isTokenBlacklisted(tokenId);
    }

    public String generateToken(UserDetails userDetails, boolean isAdmin) {
        Map<String, Object> claims = new HashMap<>();
        String tokenId = UUID.randomUUID().toString();
        
        claims.put("tokenId", tokenId);
        claims.put("isAdmin", isAdmin);
        claims.put("createdAt", new Date().getTime());
        claims.put("type", "Bearer");
        claims.put("username", userDetails.getUsername());
        
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList());

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        Date issuedAt = new Date(now);
        Date expiryDate = new Date(now + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token) && !isTokenBlacklisted(token));
    }
    
    public void invalidateToken(String token) {
        try {
            String tokenId = extractTokenId(token);
            
            // Verificar que el tokenId no sea nulo (podría pasar si el formato del token es inválido)
            if (tokenId == null) {
                throw new IllegalArgumentException("Token ID no encontrado en el token");
            }
            
            String username = extractUsername(token);
            Date expirationDate = extractExpiration(token);
            tokenBlacklistService.blacklistToken(tokenId, expirationDate, username);
        } catch (ExpiredJwtException e) {
            // Si el token ya expiró, no hay necesidad de bloquearlo
            // Pero no lanzamos excepción, porque el objetivo de invalidar se logra
        } catch (MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            // Reenviar la excepción para que el controlador la maneje adecuadamente
            throw new IllegalArgumentException("Token inválido: " + e.getMessage(), e);
        }
    }

    public Boolean isAdmin(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("isAdmin", Boolean.class);
    }
} 