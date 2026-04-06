package com.linkshort.config;

import com.linkshort.filter.JwtAuthFilter;
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

/**
 * Spring Security configuration.
 *
 * DESIGN:
 * - Stateless (no sessions) — JWT-only auth
 * - CSRF disabled (API-only, no form submissions)
 * - Public endpoints: auth, shorten, redirect, QR, analytics
 * - Protected endpoints: /api/urls/my (requires login)
 * - Anonymous users can use the shortener fully
 * - Logged-in users get extra features (link management)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — everyone can use these
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/shorten").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/shorten/bulk").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/urls").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/analytics/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/qr/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/{shortCode}").permitAll()
                .requestMatchers(HttpMethod.POST, "/{shortCode}/verify").permitAll()
                // H2 console (dev only)
                .requestMatchers("/h2-console/**").permitAll()
                // Protected endpoints — login required
                .requestMatchers("/api/urls/my").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/urls/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/urls/**").authenticated()
                // Everything else is public
                .anyRequest().permitAll()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // H2 console
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
