package com.linkshort.exception;

/**
 * Thrown when a short code does not exist in the database.
 */
public class UrlNotFoundException extends RuntimeException {
    public UrlNotFoundException(String shortCode) {
        super("URL not found for short code: " + shortCode);
    }
}
