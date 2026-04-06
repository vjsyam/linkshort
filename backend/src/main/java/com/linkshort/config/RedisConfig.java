package com.linkshort.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis configuration — only activates when app.cache.enabled=true.
 *
 * Uses StringRedisTemplate for simplicity:
 * - Keys are strings like "url:abc123"
 * - Values are the original URLs (strings)
 *
 * WHY CONDITIONAL?
 * - In local dev without Docker, Redis isn't available
 * - The app gracefully works without Redis (falls back to DB)
 * - Set app.cache.enabled=true in production when Redis is running
 */
@Configuration
@ConditionalOnProperty(name = "app.cache.enabled", havingValue = "true")
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
