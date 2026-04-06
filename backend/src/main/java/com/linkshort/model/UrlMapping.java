package com.linkshort.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Core entity representing a URL mapping.
 *
 * v2 additions: userId (optional owner), title, password, isActive toggle.
 */
@Entity
@Table(name = "url_mappings", indexes = {
    @Index(name = "idx_short_code", columnList = "short_code", unique = true),
    @Index(name = "idx_user_id", columnList = "user_id")
})
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    // v2 fields
    @Column(name = "user_id")
    private Long userId;  // nullable — anonymous URLs have no owner

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "link_password", length = 100)
    private String password;  // nullable — unprotected if null

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public UrlMapping() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.clickCount == null) this.clickCount = 0L;
        if (this.isActive == null) this.isActive = true;
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDateTime.now().isAfter(expiryDate);
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public Long getClickCount() { return clickCount; }
    public void setClickCount(Long clickCount) { this.clickCount = clickCount; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
