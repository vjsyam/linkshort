package com.linkshort.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/**
 * Utility to detect the machine's LAN IP address at runtime.
 *
 * WHY: QR codes with "localhost" are useless on other devices.
 * This detects the actual network IP (e.g., 192.168.1.x) so
 * QR codes are scannable from phones on the same WiFi.
 */
public class NetworkUtils {

    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    /**
     * Detects the machine's LAN IP address.
     * Prefers non-loopback, non-link-local, site-local IPv4 addresses.
     * Falls back to "localhost" if nothing found.
     */
    public static String detectLanIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (ni.isLoopback() || !ni.isUp()) continue;

                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    // Only IPv4. And prefer site-local (192.168.x.x, 10.x.x.x, 172.16-31.x.x)
                    if (addr.getHostAddress().contains(":")) continue; // Skip IPv6
                    if (addr.isLoopbackAddress()) continue;
                    if (addr.isSiteLocalAddress()) {
                        log.info("Detected LAN IP: {}", addr.getHostAddress());
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to detect LAN IP: {}", e.getMessage());
        }

        log.warn("No LAN IP found, falling back to localhost");
        return "localhost";
    }
}
