package com.linkshort.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for URL analytics — includes click statistics,
 * time-series data, and device/browser/referrer breakdown.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UrlStatsResponse {

    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;
    private Long totalClicks;
    private List<DailyClickCount> clicksOverTime;

    // v2: rich breakdowns
    private List<BreakdownItem> browsers;
    private List<BreakdownItem> devices;
    private List<BreakdownItem> referrers;
    private Long uniqueVisitors;

    public UrlStatsResponse() {}

    // --- Getters and Setters ---
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Long getTotalClicks() { return totalClicks; }
    public void setTotalClicks(Long totalClicks) { this.totalClicks = totalClicks; }

    public List<DailyClickCount> getClicksOverTime() { return clicksOverTime; }
    public void setClicksOverTime(List<DailyClickCount> clicksOverTime) { this.clicksOverTime = clicksOverTime; }

    public List<BreakdownItem> getBrowsers() { return browsers; }
    public void setBrowsers(List<BreakdownItem> browsers) { this.browsers = browsers; }

    public List<BreakdownItem> getDevices() { return devices; }
    public void setDevices(List<BreakdownItem> devices) { this.devices = devices; }

    public List<BreakdownItem> getReferrers() { return referrers; }
    public void setReferrers(List<BreakdownItem> referrers) { this.referrers = referrers; }

    public Long getUniqueVisitors() { return uniqueVisitors; }
    public void setUniqueVisitors(Long uniqueVisitors) { this.uniqueVisitors = uniqueVisitors; }

    /**
     * Represents click count for a single day.
     */
    public static class DailyClickCount {
        private String date;
        private Long clicks;

        public DailyClickCount() {}
        public DailyClickCount(String date, Long clicks) { this.date = date; this.clicks = clicks; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Long getClicks() { return clicks; }
        public void setClicks(Long clicks) { this.clicks = clicks; }
    }

    /**
     * Represents a category breakdown (browser name, device type, referrer).
     */
    public static class BreakdownItem {
        private String name;
        private Long count;
        private Double percentage;

        public BreakdownItem() {}
        public BreakdownItem(String name, Long count, Double percentage) {
            this.name = name; this.count = count; this.percentage = percentage;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }
}
