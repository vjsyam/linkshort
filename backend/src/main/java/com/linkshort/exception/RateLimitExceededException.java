package com.linkshort.exception;

/**
 * Thrown when a client exceeds the configured rate limit.
 */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Rate limit exceeded. Please try again later.");
    }
}
