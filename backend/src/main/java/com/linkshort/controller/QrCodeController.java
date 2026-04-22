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
     * GET /api/qr/{shortCode}
     *
     * Generates and returns a QR code PNG image for the given short code.
     * Content-Type: image/png
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable @Pattern(regexp = "^[a-zA-Z0-9_-]{1,20}$", message = "Invalid short code") String shortCode)
            throws WriterException, IOException {

        byte[] qrImage = qrCodeService.generateQrCode(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(qrImage.length);

        return new ResponseEntity<>(qrImage, headers, HttpStatus.OK);
    }
}

