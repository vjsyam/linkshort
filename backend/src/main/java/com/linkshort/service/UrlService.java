package com.linkshort.service;

import com.linkshort.dto.ShortenRequest;
import com.linkshort.dto.ShortenResponse;
import com.linkshort.exception.CustomAliasExistsException;
import com.linkshort.exception.UrlExpiredException;
import com.linkshort.exception.UrlNotFoundException;
import com.linkshort.model.ClickEvent;
import com.linkshort.model.UrlMapping;
import com.linkshort.repository.ClickEventRepository;
import com.linkshort.repository.UrlRepository;
import com.linkshort.util.Base62;
import com.linkshort.util.NetworkUtils;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core service handling URL shortening, redirect resolution,
 * and link management (toggle, delete).
 *
 * v2: Uses auto-detected LAN IP for QR-scannable short URLs.
 */
@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);
    private static final String CACHE_PREFIX = "url:";

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${app.base-url:auto}")
    private String configuredBaseUrl;

    @Value("${app.cache.enabled:false}")
    private boolean cacheEnabled;

    @Value("${app.cache.ttl-minutes:60}")
    private long cacheTtlMinutes;

    @Value("${app.default-expiry-days:365}")
    private int defaultExpiryDays;

    @Value("${server.port:8080}")
    private int serverPort;

    private String baseUrl;

    public UrlService(UrlRepository urlRepository, ClickEventRepository clickEventRepository) {
        this.urlRepository = urlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    /**
     * Auto-detect LAN IP at startup if base-url is set to "auto".
     */
    @PostConstruct
    public void init() {
        if ("auto".equals(configuredBaseUrl)) {
            String lanIp = NetworkUtils.detectLanIp();
            this.baseUrl = "http://" + lanIp + ":" + serverPort;
            log.info("Auto-detected base URL: {}", this.baseUrl);
        } else {
            this.baseUrl = configuredBaseUrl;
            log.info("Using configured base URL: {}", this.baseUrl);
        }
    }

    /**
     * Shortens a URL. userId is optional (null for anonymous users).
     */
    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request, Long userId) {
        String customAlias = request.getCustomAlias();

        // Custom alias path
        if (customAlias != null && !customAlias.isBlank()) {
            if (urlRepository.existsByShortCode(customAlias)) {
                throw new CustomAliasExistsException(customAlias);
            }
            return createMapping(request, customAlias, userId);
        }

        // Auto-generated path
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(request.getOriginalUrl());
        mapping.setShortCode("temp");
        mapping.setUserId(userId);
        mapping.setTitle(request.getTitle());
        mapping.setPassword(request.getPassword());

        if (request.getExpiryMinutes() != null) {
            mapping.setExpiryDate(LocalDateTime.now().plusMinutes(request.getExpiryMinutes()));
        } else {
            mapping.setExpiryDate(LocalDateTime.now().plusDays(defaultExpiryDays));
        }

        mapping = urlRepository.save(mapping);
        String shortCode = Base62.encode(mapping.getId());
        mapping.setShortCode(shortCode);
        mapping = urlRepository.save(mapping);

        log.info("Created short URL: {} → {}", shortCode, request.getOriginalUrl());
        cacheUrl(shortCode, request.getOriginalUrl());

        return buildResponse(mapping);
    }

    /**
     * Resolves a short code to its original URL.
     */
    @Transactional(readOnly = true)
    public String resolveUrl(String shortCode) {
        if (cacheEnabled && redisTemplate != null) {
            try {
                String cached = redisTemplate.opsForValue().get(CACHE_PREFIX + shortCode);
                if (cached != null) {
                    log.debug("Cache HIT for: {}", shortCode);
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Redis read failed: {}", e.getMessage());
            }
        }

        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (mapping.isExpired()) {
            throw new UrlExpiredException(shortCode);
        }

        if (!mapping.getIsActive()) {
            throw new UrlNotFoundException(shortCode);
        }

        cacheUrl(shortCode, mapping.getOriginalUrl());
        return mapping.getOriginalUrl();
    }

    /**
     * Checks if a short code requires a password.
     */
    @Transactional(readOnly = true)
    public boolean requiresPassword(String shortCode) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return mapping.getPassword() != null && !mapping.getPassword().isBlank();
    }

    /**
     * Verifies the password for a protected link.
     */
    @Transactional(readOnly = true)
    public boolean verifyPassword(String shortCode, String password) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
        return mapping.getPassword() != null && mapping.getPassword().equals(password);
    }

    /**
     * Records a click event asynchronously.
     */
    @Async
    @Transactional
    public void recordClick(String shortCode, HttpServletRequest request) {
        try {
            urlRepository.incrementClickCount(shortCode);

            ClickEvent event = new ClickEvent();
            event.setShortCode(shortCode);
            event.setIpAddress(getClientIp(request));
            event.setUserAgent(request.getHeader("User-Agent"));
            event.setReferer(request.getHeader("Referer"));

            clickEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }

    /**
     * Gets all shortened URLs (public list).
     */
    public List<ShortenResponse> getAllUrls() {
        return urlRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets URLs owned by a specific user.
     */
    public List<ShortenResponse> getUrlsByUser(Long userId) {
        return urlRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    /**
     * Toggle link active/inactive.
     */
    @Transactional
    public ShortenResponse toggleActive(String shortCode, Long userId) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (!userId.equals(mapping.getUserId())) {
            throw new IllegalArgumentException("You don't own this link");
        }

        mapping.setIsActive(!mapping.getIsActive());
        mapping = urlRepository.save(mapping);
        return buildResponse(mapping);
    }

    /**
     * Delete a link.
     */
    @Transactional
    public void deleteUrl(String shortCode, Long userId) {
        UrlMapping mapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        if (!userId.equals(mapping.getUserId())) {
            throw new IllegalArgumentException("You don't own this link");
        }

        urlRepository.delete(mapping);
        // Evict from cache
        if (cacheEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(CACHE_PREFIX + shortCode);
            } catch (Exception e) {
                log.warn("Redis delete failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Bulk shorten URLs.
     */
    @Transactional
    public List<ShortenResponse> bulkShorten(List<String> urls, Long userId) {
        List<ShortenResponse> responses = new ArrayList<>();
        for (String url : urls) {
            ShortenRequest req = new ShortenRequest();
            req.setOriginalUrl(url);
            responses.add(shortenUrl(req, userId));
        }
        return responses;
    }

    /**
     * Claim anonymous URLs — transfer ownership to a user.
     * Only claims URLs that have no owner (userId == null).
     */
    @Transactional
    public int claimUrls(List<String> shortCodes, Long userId) {
        int claimed = 0;
        for (String code : shortCodes) {
            urlRepository.findByShortCode(code).ifPresent(mapping -> {
                if (mapping.getUserId() == null) {
                    mapping.setUserId(userId);
                    urlRepository.save(mapping);
                }
            });
            claimed++;
        }
        return claimed;
    }

    // --- Private helpers ---

    private ShortenResponse createMapping(ShortenRequest request, String shortCode, Long userId) {
        UrlMapping mapping = new UrlMapping();
        mapping.setOriginalUrl(request.getOriginalUrl());
        mapping.setShortCode(shortCode);
        mapping.setUserId(userId);
        mapping.setTitle(request.getTitle());
        mapping.setPassword(request.getPassword());

        if (request.getExpiryMinutes() != null) {
            mapping.setExpiryDate(LocalDateTime.now().plusMinutes(request.getExpiryMinutes()));
        } else {
            mapping.setExpiryDate(LocalDateTime.now().plusDays(defaultExpiryDays));
        }

        mapping = urlRepository.save(mapping);
        cacheUrl(shortCode, request.getOriginalUrl());
        log.info("Created custom short URL: {} → {}", shortCode, request.getOriginalUrl());
        return buildResponse(mapping);
    }

    private void cacheUrl(String shortCode, String originalUrl) {
        if (cacheEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(
                        CACHE_PREFIX + shortCode, originalUrl,
                        cacheTtlMinutes, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("Redis cache write failed: {}", e.getMessage());
            }
        }
    }

    private ShortenResponse buildResponse(UrlMapping mapping) {
        ShortenResponse response = new ShortenResponse();
        response.setShortCode(mapping.getShortCode());
        response.setShortUrl(baseUrl + "/" + mapping.getShortCode());
        response.setOriginalUrl(mapping.getOriginalUrl());
        response.setCreatedAt(mapping.getCreatedAt());
        response.setExpiryDate(mapping.getExpiryDate());
        response.setClickCount(mapping.getClickCount());
        response.setTitle(mapping.getTitle());
        response.setIsActive(mapping.getIsActive());
        response.setHasPassword(mapping.getPassword() != null && !mapping.getPassword().isBlank());
        return response;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
