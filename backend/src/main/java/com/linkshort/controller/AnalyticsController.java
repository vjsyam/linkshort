package com.linkshort.controller;

import com.linkshort.dto.UrlStatsResponse;
import com.linkshort.service.AnalyticsService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for URL analytics.
 * GET /api/analytics/{shortCode} → Returns click statistics + breakdowns.
 * Only authenticated link owners can view their analytics.
 */
@RestController
@RequestMapping("/api/analytics")
@Validated
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getAnalytics(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,20}$", message = "Invalid short code") String shortCode) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(analyticsService.getStats(shortCode, userId));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long) {
            return (Long) auth.getPrincipal();
        }
        return null;
    }
}

