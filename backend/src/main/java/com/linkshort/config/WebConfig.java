package com.linkshort.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration.
 *
 * NOTE: CORS is configured exclusively in SecurityConfig
 * (reads allowed origins from env vars). Do NOT add CORS
 * mappings here — duplicate config causes conflicts.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS handled by SecurityConfig.corsConfigurationSource()
}

