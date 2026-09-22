package com.linkshort.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * QR Code generation service using ZXing (Zebra Crossing).
 *
 * Generates PNG images encoding the short URL, so users can scan
 * with their phone camera to navigate to the shortened link.
 *
 * ERROR CORRECTION LEVEL: H (High - 30% recovery)
 * - Allows the QR code to remain scannable even if partially obscured
 * - Trade-off: slightly denser pattern, but more robust
 */
@Service
public class QrCodeService {

    private static final Logger log = LoggerFactory.getLogger(QrCodeService.class);
    private static final int QR_SIZE = 300;  // 300x300 pixels

    private final UrlService urlService;

    public QrCodeService(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Generate a QR code PNG image for a short code.
     *
     * @param shortCode the short code to encode in the QR
     * @param target "short" for short redirect URL, "direct" for original destination URL
     * @return PNG image as byte array
     */
    public byte[] generateQrCode(String shortCode, String target) throws WriterException, IOException {
        String targetUrl;
        if ("direct".equalsIgnoreCase(target)) {
            targetUrl = urlService.getOriginalUrl(shortCode);
        } else {
            targetUrl = urlService.getShortUrl(shortCode);
        }
        return generateQrCodeForUrl(targetUrl);
    }

    public byte[] generateQrCode(String shortCode) throws WriterException, IOException {
        return generateQrCode(shortCode, "short");
    }

    /**
     * Generate a QR code PNG image directly for any URL.
     */
    public byte[] generateQrCodeForUrl(String url) throws WriterException, IOException {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL to encode in QR code cannot be empty");
        }

        // Configure QR code parameters
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 2);  // Quiet zone around the QR code
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // Generate the QR code matrix
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(url, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

        // Convert to PNG image
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

        log.debug("Generated QR code for: {}", url);
        return outputStream.toByteArray();
    }
}
