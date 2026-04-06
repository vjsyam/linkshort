package com.linkshort.controller;

import com.linkshort.dto.UrlStatsResponse;
import com.linkshort.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for URL analytics.
 * GET /api/analytics/{shortCode} → Returns click statistics + breakdowns.
 * Only link owners (or anonymous owners) can view their analytics.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<UrlStatsResponse> getAnalytics(@PathVariable String shortCode) {
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
