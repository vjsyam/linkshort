package com.linkshort.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating shortened URLs.
 *
 * Supports both single and bulk URL shortening.
 * All fields except originalUrl are optional.
 */
public class ShortenRequest {

    @NotBlank(message = "URL is required")
    @Pattern(
        regexp = "^https?://.*",
        message = "URL must start with http:// or https://"
    )
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String originalUrl;

    @Pattern(
        regexp = "^[a-zA-Z0-9_-]{3,20}$",
        message = "Custom alias must be 3-20 alphanumeric characters (hyphens and underscores allowed)"
    )
    private String customAlias;

    @Positive(message = "Expiry must be a positive number of minutes")
    private Integer expiryMinutes;

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;       // User-editable label

    @Size(max = 100, message = "Password must not exceed 100 characters")
    private String password;    // Optional password protection

    public ShortenRequest() {}

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }

    public String getCustomAlias() { return customAlias; }
    public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }

    public Integer getExpiryMinutes() { return expiryMinutes; }
    public void setExpiryMinutes(Integer expiryMinutes) { this.expiryMinutes = expiryMinutes; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
