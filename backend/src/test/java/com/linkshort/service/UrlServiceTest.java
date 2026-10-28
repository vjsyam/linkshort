package com.linkshort.service;

import com.linkshort.dto.ShortenRequest;
import com.linkshort.dto.ShortenResponse;
import com.linkshort.exception.UrlExpiredException;
import com.linkshort.exception.UrlNotFoundException;
import com.linkshort.model.UrlMapping;
import com.linkshort.repository.ClickEventRepository;
import com.linkshort.repository.UrlRepository;
import com.linkshort.util.Base62;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        urlService = new UrlService(urlRepository, clickEventRepository, passwordEncoder);
        ReflectionTestUtils.setField(urlService, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(urlService, "cacheEnabled", true);
        ReflectionTestUtils.setField(urlService, "cacheTtlMinutes", 60L);
        ReflectionTestUtils.setField(urlService, "defaultExpiryDays", 365);
        ReflectionTestUtils.setField(urlService, "configuredBaseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(urlService, "serverPort", 8080);
        urlService.init();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("generates deterministic Base62 shortcode from entity ID")
    void shortenUrlGeneratesBase62Code() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com/test-url");

        long generatedId = 12345L;
        String expectedShortCode = Base62.encode(generatedId);

        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            if (mapping.getId() == null) {
                mapping.setId(generatedId);
            }
            return mapping;
        });

        ShortenResponse response = urlService.shortenUrl(request, 1L);

        assertNotNull(response);
        assertEquals(expectedShortCode, response.getShortCode());
        assertEquals("https://example.com/test-url", response.getOriginalUrl());
        assertEquals("http://localhost:8080/" + expectedShortCode, response.getShortUrl());
        verify(urlRepository, times(2)).save(any(UrlMapping.class));
    }

    @Test
    @DisplayName("encodes link password with BCrypt during shortening")
    void shortenUrlHashesPassword() {
        ShortenRequest request = new ShortenRequest();
        request.setOriginalUrl("https://example.com/protected");
        request.setPassword("securePassword123");

        when(passwordEncoder.encode("securePassword123")).thenReturn("$2a$10$hashedPasswordHere");
        when(urlRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> {
            UrlMapping mapping = invocation.getArgument(0);
            if (mapping.getId() == null) {
                mapping.setId(99L);
            }
            return mapping;
        });

        ShortenResponse response = urlService.shortenUrl(request, null);

        assertTrue(response.getHasPassword());
        verify(passwordEncoder).encode("securePassword123");
    }

    @Test
    @DisplayName("cache hit: resolves original URL from Redis without reading PostgreSQL")
    void resolveUrlCacheHit() {
        String shortCode = "hitCode";
        String expectedUrl = "https://example.com/cached";

        when(valueOperations.get("url:" + shortCode)).thenReturn(expectedUrl);

        String result = urlService.resolveUrl(shortCode);

        assertEquals(expectedUrl, result);
        verify(valueOperations).get("url:" + shortCode);
        verifyNoInteractions(urlRepository);
    }

    @Test
    @DisplayName("cache miss: reads PostgreSQL and populates Redis cache")
    void resolveUrlCacheMissPopulatesRedis() {
        String shortCode = "missCode";
        String originalUrl = "https://example.com/from-db";

        when(valueOperations.get("url:" + shortCode)).thenReturn(null);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(50L);
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl(originalUrl);
        mapping.setIsActive(true);
        mapping.setExpiryDate(LocalDateTime.now().plusDays(10));

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        String result = urlService.resolveUrl(shortCode);

        assertEquals(originalUrl, result);
        verify(valueOperations).get("url:" + shortCode);
        verify(urlRepository).findByShortCode(shortCode);
        verify(valueOperations).set("url:" + shortCode, originalUrl, 60L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("throws UrlExpiredException when link has expired")
    void resolveUrlThrowsWhenExpired() {
        String shortCode = "expiredCode";

        when(valueOperations.get("url:" + shortCode)).thenReturn(null);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(51L);
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl("https://example.com/expired");
        mapping.setIsActive(true);
        mapping.setExpiryDate(LocalDateTime.now().minusHours(2)); // Expired

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        assertThrows(UrlExpiredException.class, () -> urlService.resolveUrl(shortCode));
    }

    @Test
    @DisplayName("throws UrlNotFoundException when link is inactive")
    void resolveUrlThrowsWhenInactive() {
        String shortCode = "inactiveCode";

        when(valueOperations.get("url:" + shortCode)).thenReturn(null);

        UrlMapping mapping = new UrlMapping();
        mapping.setId(52L);
        mapping.setShortCode(shortCode);
        mapping.setOriginalUrl("https://example.com/inactive");
        mapping.setIsActive(false); // Inactive
        mapping.setExpiryDate(LocalDateTime.now().plusDays(5));

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        assertThrows(UrlNotFoundException.class, () -> urlService.resolveUrl(shortCode));
    }

    @Test
    @DisplayName("throws UrlNotFoundException when link does not exist in repository")
    void resolveUrlThrowsWhenNotFound() {
        String shortCode = "nonExistent";

        when(valueOperations.get("url:" + shortCode)).thenReturn(null);
        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.resolveUrl(shortCode));
    }

    @Test
    @DisplayName("deleting a link deletes entity and evicts Redis cache")
    void deleteUrlEvictsCache() {
        String shortCode = "deleteMe";
        Long userId = 10L;

        UrlMapping mapping = new UrlMapping();
        mapping.setId(53L);
        mapping.setShortCode(shortCode);
        mapping.setUserId(userId);

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(mapping));

        urlService.deleteUrl(shortCode, userId);

        verify(urlRepository).delete(mapping);
        verify(redisTemplate).delete("url:" + shortCode);
    }
}
