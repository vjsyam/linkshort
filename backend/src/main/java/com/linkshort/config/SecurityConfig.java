package com.linkshort.config;

import com.linkshort.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration.
 *
 * DESIGN:
 * - Stateless (no sessions) — JWT-only auth
 * - CSRF disabled (API-only, no form submissions)
 * - CORS enabled for cross-origin deployment (Vercel → Render)
 * - Public endpoints: auth, shorten, redirect, QR
 * - Protected endpoints: /api/urls/my, analytics (ownership checked)
 *
 * SECURITY HEADERS:
 * - X-Frame-Options: DENY (clickjacking protection)
 * - X-Content-Type-Options: nosniff (MIME-type sniffing protection)
 * - Referrer-Policy: strict-origin-when-cross-origin
 * - HSTS: enforces HTTPS for 1 year (including subdomains)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/shorten").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/shorten/bulk").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/qr/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/{shortCode}").permitAll()
                .requestMatchers(HttpMethod.POST, "/{shortCode}/verify").permitAll()
                // Protected endpoints
                .requestMatchers("/api/urls/my").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/analytics/**").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/urls/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/urls/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/urls/claim").authenticated()
                .anyRequest().permitAll()
            )
            // Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(cto -> {})  // X-Content-Type-Options: nosniff (enabled by default)
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000)  // 1 year
                )
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                )
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        if ("*".equals(allowedOrigins)) {
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

