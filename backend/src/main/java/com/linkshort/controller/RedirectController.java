package com.linkshort.controller;

import com.linkshort.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

/**
 * Controller handling short URL redirects.
 *
 * GET /{shortCode} → 302 redirect (or password prompt if protected)
 * POST /{shortCode}/verify → Password verification for protected links
 */
@RestController
@Validated
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);
    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,20}$", message = "Invalid short code") String shortCode,
            HttpServletRequest request) {

        log.info("Redirect request for: {}", shortCode);

        // Check if link requires password
        if (urlService.requiresPassword(shortCode)) {
            // Return a JSON response indicating password is required
            // The frontend will show a password prompt
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        "passwordRequired", true,
                        "shortCode", shortCode,
                        "message", "This link is password protected"
                    ));
        }

        String originalUrl = urlService.resolveUrl(shortCode);
        urlService.recordClick(shortCode, request);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * POST /{shortCode}/verify — Verify password and redirect.
     */
    @PostMapping("/{shortCode}/verify")
    public ResponseEntity<?> verifyAndRedirect(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,20}$", message = "Invalid short code") String shortCode,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        String password = body.get("password");
        if (password == null || !urlService.verifyPassword(shortCode, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Incorrect password"));
        }

        String originalUrl = urlService.resolveUrl(shortCode);
        urlService.recordClick(shortCode, request);

        return ResponseEntity.ok(Map.of("redirectUrl", originalUrl));
    }
}
