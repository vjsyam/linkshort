package com.linkshort.exception;

/**
 * Thrown when a shortened URL has passed its expiry date.
 */
public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortCode) {
        super("URL has expired for short code: " + shortCode);
    }
}
