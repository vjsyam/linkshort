package com.linkshort.controller;

import com.linkshort.dto.BulkShortenRequest;
import com.linkshort.dto.ShortenRequest;
import com.linkshort.dto.ShortenResponse;
import com.linkshort.service.UrlService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for URL shortening operations.
 *
 * Public:  POST /api/shorten, POST /api/shorten/bulk, GET /api/urls
 * Auth:    GET /api/urls/my, PATCH /api/urls/{code}/toggle, DELETE /api/urls/{code}
 */
@RestController
@RequestMapping("/api")
public class UrlController {

    private static final Logger log = LoggerFactory.getLogger(UrlController.class);
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        Long userId = getCurrentUserId();
        log.info("Shorten request: {} (user: {})", request.getOriginalUrl(), userId);
        ShortenResponse response = urlService.shortenUrl(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shorten/bulk")
    public ResponseEntity<List<ShortenResponse>> bulkShorten(@Valid @RequestBody BulkShortenRequest request) {
        Long userId = getCurrentUserId();
        log.info("Bulk shorten: {} URLs (user: {})", request.getUrls().size(), userId);
        List<ShortenResponse> responses = urlService.bulkShorten(request.getUrls(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/urls")
    public ResponseEntity<List<ShortenResponse>> getAllUrls() {
        return ResponseEntity.ok(urlService.getAllUrls());
    }

    @GetMapping("/urls/my")
    public ResponseEntity<List<ShortenResponse>> getMyUrls() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(urlService.getUrlsByUser(userId));
    }

    @PatchMapping("/urls/{shortCode}/toggle")
    public ResponseEntity<ShortenResponse> toggleActive(@PathVariable String shortCode) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(urlService.toggleActive(shortCode, userId));
    }

    @DeleteMapping("/urls/{shortCode}")
    public ResponseEntity<Map<String, String>> deleteUrl(@PathVariable String shortCode) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        urlService.deleteUrl(shortCode, userId);
        return ResponseEntity.ok(Map.of("message", "Link deleted successfully"));
    }

    /**
     * POST /api/urls/claim
     * Claims anonymous URLs (transfers ownership to the logged-in user).
     * Called when a user creates links anonymously then logs in.
     */
    @PostMapping("/urls/claim")
    public ResponseEntity<Map<String, Object>> claimUrls(@RequestBody Map<String, List<String>> body) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<String> shortCodes = body.get("shortCodes");
        if (shortCodes == null || shortCodes.isEmpty()) {
            return ResponseEntity.ok(Map.of("claimed", 0));
        }
        int claimed = urlService.claimUrls(shortCodes, userId);
        log.info("Claimed {} URLs for user {}", claimed, userId);
        return ResponseEntity.ok(Map.of("claimed", claimed));
    }

    /**
     * Extract userId from SecurityContext (set by JwtAuthFilter).
     * Returns null for anonymous users.
     */
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }
}
