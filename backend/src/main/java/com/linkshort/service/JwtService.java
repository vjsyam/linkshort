package com.linkshort.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT token service.
 * Handles token generation, validation, and claim extraction.
 *
 * Uses HMAC-SHA256 with a configurable secret key.
 * Tokens contain the user's email as the subject and userId as a claim.
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret:linkshort-secret-key-must-be-at-least-32-chars-long-for-hmac-sha-256}")
    private String secret;

    @Value("${app.jwt.expiration-hours:24}")
    private int expirationHours;

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a JWT token for a user.
     */
    public String generateToken(Long userId, String email) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (long) expirationHours * 3600 * 1000))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract user email from token.
     */
    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extract userId from token.
     */
    public Long extractUserId(String token) {
        return extractClaims(token).get("userId", Long.class);
    }

    /**
     * Validate token: check signature and expiration.
     */
    public boolean isValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
