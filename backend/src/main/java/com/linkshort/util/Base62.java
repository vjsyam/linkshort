package com.linkshort.util;

/**
 * Base62 Encoder/Decoder for URL Short Code Generation.
 *
 * Converts a numeric ID (from DB auto-increment) into a compact,
 * URL-safe alphanumeric string using characters [0-9a-zA-Z].
 *
 * WHY BASE62?
 * - 62 characters = 10 digits + 26 lowercase + 26 uppercase
 * - A 7-character code can represent 62^7 = 3.5 TRILLION unique URLs
 * - No special characters that need URL encoding
 * - Collision-free: each ID maps to exactly one unique code (bijective)
 *
 * EXAMPLES:
 *   encode(1)       → "1"
 *   encode(62)      → "10"
 *   encode(1000000) → "4c92"
 */
public final class Base62 {

    // The 62-character alphabet: digits, lowercase, uppercase
    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    // Prevent instantiation — utility class
    private Base62() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Encodes a positive long value to a Base62 string.
     *
     * @param value the numeric ID to encode (must be > 0)
     * @return Base62-encoded string
     * @throws IllegalArgumentException if value <= 0
     */
    public static String encode(long value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Value must be positive, got: " + value);
        }

        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            // Get the remainder when dividing by 62 → maps to a character
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }

        // Digits are generated in reverse order, so reverse the result
        return sb.reverse().toString();
    }

    /**
     * Decodes a Base62 string back to its numeric value.
     *
     * @param encoded the Base62 string to decode
     * @return the original numeric value
     * @throws IllegalArgumentException if the string contains invalid characters
     */
    public static long decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("Encoded string must not be null or empty");
        }

        long value = 0;
        for (char c : encoded.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index == -1) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            value = value * BASE + index;
        }

        return value;
    }
}
