package com.linkshort.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for tracking individual click events.
 *
 * WHY A SEPARATE TABLE?
 * - UrlMapping.clickCount gives us a fast aggregate count (denormalized)
 * - ClickEvent gives us TIME-SERIES data for analytics charts
 * - This separation follows the CQRS pattern: fast writes, rich reads
 */
@Entity
@Table(name = "click_events", indexes = {
    @Index(name = "idx_click_short_code", columnList = "short_code"),
    @Index(name = "idx_click_timestamp", columnList = "clicked_at")
})
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 10)
    private String shortCode;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referer", length = 2048)
    private String referer;

    public ClickEvent() {}

    public ClickEvent(Long id, String shortCode, LocalDateTime clickedAt,
                      String ipAddress, String userAgent, String referer) {
        this.id = id;
        this.shortCode = shortCode;
        this.clickedAt = clickedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    @PrePersist
    protected void onCreate() {
        this.clickedAt = LocalDateTime.now();
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public LocalDateTime getClickedAt() { return clickedAt; }
    public void setClickedAt(LocalDateTime clickedAt) { this.clickedAt = clickedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getReferer() { return referer; }
    public void setReferer(String referer) { this.referer = referer; }
}
