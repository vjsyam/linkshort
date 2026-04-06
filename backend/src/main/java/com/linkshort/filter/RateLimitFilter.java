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
 * WHY IN-MEMORY (not Redis)?
 * - Works without Docker/Redis for local development
 * - For production with multiple instances, replace with Redis-backed
 *   rate limiter (use INCR + EXPIRE commands for distributed counting)
 *
 * IMPLEMENTATION:
 * - ConcurrentHashMap<IP, TokenBucket> for thread-safe concurrent access
 * - Each bucket tracks: count + window start time
 * - Stale entries are lazily cleaned up on access
 *
 * TRADE-OFF:
 * - Fixed window can have burst edge cases (up to 2x limit at window boundary)
 * - For a URL shortener, this is acceptable — sliding window adds complexity
 *   without meaningful benefit at this scale
 */
@Component
@Order(1)  // Execute before other filters
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    // Thread-safe map: IP → request tracker
    private final Map<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for static resources and H2 console
        String path = httpRequest.getRequestURI();
        if (path.startsWith("/h2-console") || path.contains(".")) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(httpRequest);

        if (isRateLimited(clientIp)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);

            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    objectMapper.writeValueAsString(
                            ErrorResponse.of(429, "Too Many Requests",
                                    "Rate limit exceeded. Max " + requestsPerMinute + " requests per minute.")
                    )
            );
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
     * Check if the IP has exceeded the rate limit.
     * Uses a fixed-window counter that resets every minute.
     */
    private boolean isRateLimited(String clientIp) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000;  // 1 minute window

        RequestTracker tracker = requestCounts.compute(clientIp, (ip, existing) -> {
            if (existing == null || existing.windowStart < windowStart) {
                // New window — reset counter
                return new RequestTracker(now, new AtomicInteger(1));
            }
            // Same window — increment counter
            existing.count.incrementAndGet();
            return existing;
        });

        return tracker.count.get() > requestsPerMinute;
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
