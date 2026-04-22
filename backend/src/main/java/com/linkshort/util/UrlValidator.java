package com.linkshort.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * URL validation utility to prevent SSRF and open redirect attacks.
 *
 * Blocks:
 * - Private/reserved IP ranges (127.x, 10.x, 192.168.x, 172.16-31.x, 169.254.x)
 * - Localhost and loopback addresses
 * - Link-local and multicast addresses
 * - Cloud metadata endpoints (169.254.169.254)
 * - URLs without valid hostnames
 * - Non-HTTP(S) schemes
 */
public final class UrlValidator {

    private static final Logger log = LoggerFactory.getLogger(UrlValidator.class);

    /**
     * Maximum allowed URL length (matches DB column size).
     */
    public static final int MAX_URL_LENGTH = 2048;

    /**
     * Hostnames that should always be blocked.
     */
    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "localhost.localdomain",
            "ip6-localhost",
            "ip6-loopback",
            "metadata.google.internal",    // GCP metadata
            "metadata.internal"            // Generic cloud metadata
    );

    private UrlValidator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validates that a URL is safe to shorten.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException if the URL is invalid or targets a blocked destination
     */
    public static void validate(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL is required");
        }

        if (url.length() > MAX_URL_LENGTH) {
            throw new IllegalArgumentException("URL exceeds maximum length of " + MAX_URL_LENGTH + " characters");
        }

        // Parse as URI to validate structure
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        // Validate scheme
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("URL must use http or https scheme");
        }

        // Validate host exists
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL must have a valid hostname");
        }

        host = host.toLowerCase().trim();

        // Check against blocked hostnames
        if (BLOCKED_HOSTNAMES.contains(host)) {
            log.warn("Blocked URL targeting reserved hostname: {}", host);
            throw new IllegalArgumentException("URLs targeting internal services are not allowed");
        }

        // Check if host is an IP address and validate it
        if (isIpAddress(host)) {
            validateIpAddress(host);
        }
    }

    /**
     * Check if a string looks like an IP address (v4 or v6).
     */
    private static boolean isIpAddress(String host) {
        // IPv6 in brackets would already be parsed by URI
        // Check for IPv4: all digits and dots
        if (host.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            return true;
        }
        // IPv6: contains colons
        return host.contains(":");
    }

    /**
     * Validates that an IP address is not in a private/reserved range.
     */
    private static void validateIpAddress(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);

            if (addr.isLoopbackAddress()) {
                throw new IllegalArgumentException("URLs targeting loopback addresses are not allowed");
            }
            if (addr.isSiteLocalAddress()) {
                throw new IllegalArgumentException("URLs targeting private network addresses are not allowed");
            }
            if (addr.isLinkLocalAddress()) {
                throw new IllegalArgumentException("URLs targeting link-local addresses are not allowed");
            }
            if (addr.isMulticastAddress()) {
                throw new IllegalArgumentException("URLs targeting multicast addresses are not allowed");
            }
            if (addr.isAnyLocalAddress()) {
                throw new IllegalArgumentException("URLs targeting wildcard addresses are not allowed");
            }

            // Explicit check for cloud metadata endpoint
            if (ip.equals("169.254.169.254")) {
                throw new IllegalArgumentException("URLs targeting cloud metadata endpoints are not allowed");
            }

        } catch (IllegalArgumentException e) {
            // Re-throw our own validation errors
            throw e;
        } catch (Exception e) {
            log.warn("Failed to validate IP address {}: {}", ip, e.getMessage());
            throw new IllegalArgumentException("Invalid IP address in URL");
        }
    }
}
