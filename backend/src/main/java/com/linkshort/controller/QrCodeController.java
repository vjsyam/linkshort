package com.linkshort.controller;

import com.linkshort.service.QrCodeService;
import com.google.zxing.WriterException;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller for QR code generation.
 *
 * GET /api/qr/{shortCode} → Returns a PNG image of the QR code
 *
 * The QR code encodes the full short URL (e.g., http://localhost:8080/abc123)
 * so scanning it with a phone camera triggers the redirect.
 */
@RestController
@RequestMapping("/api/qr")
@Validated
public class QrCodeController {

    private final QrCodeService qrCodeService;

    public QrCodeController(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    /**
     * GET /api/qr/{shortCode}?target=short|direct&url=...
     *
     * Generates and returns a QR code PNG image for the given short code.
     * target: "short" (default) encodes the short redirect URL.
     *         "direct" encodes the original destination URL.
     * url: optional explicit URL to encode directly into the QR code.
     * Content-Type: image/png
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,20}$", message = "Invalid short code") String shortCode,
            @RequestParam(required = false, defaultValue = "short") String target,
            @RequestParam(required = false) String url)
            throws WriterException, IOException {

        byte[] qrImage;
        if (url != null && !url.isBlank()) {
            qrImage = qrCodeService.generateQrCodeForUrl(url);
        } else {
            qrImage = qrCodeService.generateQrCode(shortCode, target);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrImage.length);
        headers.setCacheControl("public, max-age=86400");

        return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
    }
}

