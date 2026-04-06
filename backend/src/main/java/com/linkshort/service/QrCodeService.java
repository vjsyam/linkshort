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

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * Generate a QR code PNG image for a short code.
     *
     * @param shortCode the short code to encode in the QR
     * @return PNG image as byte array
     */
    public byte[] generateQrCode(String shortCode) throws WriterException, IOException {
        String url = baseUrl + "/" + shortCode;

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
