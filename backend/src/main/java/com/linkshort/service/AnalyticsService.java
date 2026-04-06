package com.linkshort.service;

import com.linkshort.dto.UrlStatsResponse;
import com.linkshort.exception.UrlNotFoundException;
import com.linkshort.model.ClickEvent;
import com.linkshort.model.UrlMapping;
import com.linkshort.repository.ClickEventRepository;
import com.linkshort.repository.UrlRepository;
import com.linkshort.util.NetworkUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for URL click analytics.
 * Aggregates click events into daily counts, browser/device/referrer breakdowns.
 * v2: Restricted to link owners only.
 */
@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;

    @Value("${app.base-url:auto}")
    private String configuredBaseUrl;

    @Value("${server.port:8080}")
    private int serverPort;

    private String baseUrl;

    public AnalyticsService(UrlRepository urlRepository, ClickEventRepository clickEventRepository) {
        this.urlRepository = urlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @PostConstruct
    public void init() {
        if ("auto".equals(configuredBaseUrl)) {
            String lanIp = NetworkUtils.detectLanIp();
            this.baseUrl = "http://" + lanIp + ":" + serverPort;
        } else {
            this.baseUrl = configuredBaseUrl;
        }
    }

    /**
     * Get complete analytics for a short code.
     * If userId is provided, validates ownership.
     */
    public UrlStatsResponse getStats(String shortCode, Long userId) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        // Ownership check: only the owner can view analytics
        if (userId != null && mapping.getUserId() != null && !userId.equals(mapping.getUserId())) {
            throw new IllegalArgumentException("You don't have access to this link's analytics");
        }

        // If the link has an owner and the viewer is anonymous — deny
        if (userId == null && mapping.getUserId() != null) {
            throw new IllegalArgumentException("You don't have access to this link's analytics");
        }

        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> rawData = clickEventRepository.getDailyClickCounts(shortCode, since);

        // Daily click counts
        Map<String, Long> clickMap = rawData.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        List<UrlStatsResponse.DailyClickCount> clicksOverTime = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            clicksOverTime.add(new UrlStatsResponse.DailyClickCount(
                    date, clickMap.getOrDefault(date, 0L)));
        }

        // Fetch all click events for breakdown
        List<ClickEvent> events = clickEventRepository.findByShortCodeOrderByClickedAtDesc(shortCode);
        long total = events.size();

        UrlStatsResponse response = new UrlStatsResponse();
        response.setShortCode(mapping.getShortCode());
        response.setShortUrl(baseUrl + "/" + mapping.getShortCode());
        response.setOriginalUrl(mapping.getOriginalUrl());
        response.setCreatedAt(mapping.getCreatedAt());
        response.setExpiryDate(mapping.getExpiryDate());
        response.setTotalClicks(mapping.getClickCount());
        response.setClicksOverTime(clicksOverTime);

        // Browser breakdown
        response.setBrowsers(buildBreakdown(events, this::extractBrowser, total));

        // Device breakdown
        response.setDevices(buildBreakdown(events, this::extractDevice, total));

        // Referrer breakdown
        response.setReferrers(buildBreakdown(events, e -> {
            String ref = e.getReferer();
            if (ref == null || ref.isBlank()) return "Direct";
            try {
                String host = ref.replaceAll("https?://", "").split("/")[0];
                return host.length() > 40 ? host.substring(0, 40) : host;
            } catch (Exception ex) { return "Unknown"; }
        }, total));

        // Unique visitors (by IP)
        long uniqueIps = events.stream()
                .map(ClickEvent::getIpAddress)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        response.setUniqueVisitors(uniqueIps);

        return response;
    }

    // Overload for backward compat (public analytics, no user check)
    public UrlStatsResponse getStats(String shortCode) {
        return getStats(shortCode, null);
    }

    // --- User-Agent parsing ---

    private String extractBrowser(ClickEvent e) {
        String ua = e.getUserAgent();
        if (ua == null || ua.isBlank()) return "Unknown";
        ua = ua.toLowerCase();
        if (ua.contains("edg/") || ua.contains("edge")) return "Edge";
        if (ua.contains("opr/") || ua.contains("opera")) return "Opera";
        if (ua.contains("chrome") && !ua.contains("edg")) return "Chrome";
        if (ua.contains("firefox")) return "Firefox";
        if (ua.contains("safari") && !ua.contains("chrome")) return "Safari";
        if (ua.contains("curl")) return "cURL";
        if (ua.contains("postman")) return "Postman";
        return "Other";
    }

    private String extractDevice(ClickEvent e) {
        String ua = e.getUserAgent();
        if (ua == null || ua.isBlank()) return "Unknown";
        ua = ua.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "Mobile";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) return "Bot";
        return "Desktop";
    }

    private List<UrlStatsResponse.BreakdownItem> buildBreakdown(
            List<ClickEvent> events,
            java.util.function.Function<ClickEvent, String> classifier,
            long total) {

        Map<String, Long> grouped = events.stream()
                .map(classifier)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> new UrlStatsResponse.BreakdownItem(
                        entry.getKey(),
                        entry.getValue(),
                        total > 0 ? Math.round(entry.getValue() * 1000.0 / total) / 10.0 : 0.0
                ))
                .collect(Collectors.toList());
    }
}
