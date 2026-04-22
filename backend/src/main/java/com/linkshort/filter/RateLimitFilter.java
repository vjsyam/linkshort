package com.linkshort.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkshort.dto.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP-based rate limiting filter using a sliding window counter.
 *
 * ALGORITHM: Fixed window counter (per minute)
 * - Tracks request count per IP address per minute
 * - Rejects requests exceeding the configured limit with HTTP 429
 *
 * AUTH PROTECTION:
 * - Auth endpoints (/api/auth/) have a separate, stricter rate limit
 *   to prevent brute-force login and credential stuffing attacks.
 *   Default: 5 auth requests per minute per IP.
 *
 * WHY IN-MEMORY (not Redis)?
 * - Works without Docker/Redis for local development
 * - For production with multiple instances, replace with Redis-backed
 *   rate limiter (use INCR + EXPIRE commands for distributed counting)
 */
@Component
@Order(1)  // Execute before other filters
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:5}")
    private int authRequestsPerMinute;

    // Thread-safe maps: IP → request tracker
    private final Map<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, RequestTracker> authRequestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for static resources
        String path = httpRequest.getRequestURI();
        if (path.contains(".")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);

        // Stricter rate limit for auth endpoints (brute-force protection)
        if (path.startsWith("/api/auth/")) {
            if (isRateLimited(clientIp, authRequestCounts, authRequestsPerMinute)) {
                log.warn("Auth rate limit exceeded for IP: {}", clientIp);
                sendRateLimitResponse(httpResponse, authRequestsPerMinute);
                return;
            }
        }

        // General rate limit
        if (isRateLimited(clientIp, requestCounts, requestsPerMinute)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            sendRateLimitResponse(httpResponse, requestsPerMinute);
            return;
        }

        // Add rate limit headers for client awareness
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        RequestTracker tracker = requestCounts.get(clientIp);
        if (tracker != null) {
            httpResponse.setHeader("X-RateLimit-Remaining",
                    String.valueOf(Math.max(0, requestsPerMinute - tracker.count.get())));
        }

        chain.doFilter(request, response);
    }

    /**
     * Check if the IP has exceeded the rate limit for the given tracking map.
     */
    private boolean isRateLimited(String clientIp, Map<String, RequestTracker> trackingMap, int limit) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000;  // 1 minute window

        RequestTracker tracker = trackingMap.compute(clientIp, (ip, existing) -> {
            if (existing == null || existing.windowStart < windowStart) {
                return new RequestTracker(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return tracker.count.get() > limit;
    }

    private void sendRateLimitResponse(HttpServletResponse httpResponse, int limit) throws IOException {
        httpResponse.setStatus(429);
        httpResponse.setContentType("application/json");
        httpResponse.getWriter().write(
                objectMapper.writeValueAsString(
                        ErrorResponse.of(429, "Too Many Requests",
                                "Rate limit exceeded. Max " + limit + " requests per minute.")
                )
        );
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Tracks request count within a time window for a single IP.
     */
    private static class RequestTracker {
        final long windowStart;
        final AtomicInteger count;

        RequestTracker(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

