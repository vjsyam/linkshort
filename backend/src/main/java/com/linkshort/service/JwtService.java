package com.linkshort.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String DEFAULT_PLACEHOLDER = "CHANGE_ME_IN_PRODUCTION";

    @Value("${app.jwt.secret:CHANGE_ME_IN_PRODUCTION}")
    private String secret;

    @Value("${app.jwt.expiration-hours:24}")
    private int expirationHours;

    /**
     * Validate JWT secret at startup.
     * Rejects the default placeholder and secrets shorter than 32 characters.
     */
    @PostConstruct
    public void validateSecret() {
        if (DEFAULT_PLACEHOLDER.equals(secret)) {
            log.error("\n" +
                "============================================================\n" +
                "  FATAL: JWT_SECRET is not configured!\n" +
                "  Set the JWT_SECRET environment variable to a random\n" +
                "  string of at least 32 characters.\n" +
                "  Generate one: openssl rand -base64 48\n" +
                "============================================================");
            throw new IllegalStateException("JWT_SECRET must be configured. See logs for details.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long (got " + secret.length() + ")");
        }
        log.info("JWT secret validated (length: {} chars)", secret.length());
    }

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
