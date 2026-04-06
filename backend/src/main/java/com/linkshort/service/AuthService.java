package com.linkshort.service;

import com.linkshort.dto.AuthRequest;
import com.linkshort.dto.AuthResponse;
import com.linkshort.dto.RegisterRequest;
import com.linkshort.model.User;
import com.linkshort.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Authentication service handling registration and login.
 *
 * Registration: hash password with BCrypt, save user, issue JWT.
 * Login: verify credentials, issue JWT.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Register a new user.
     * @throws IllegalArgumentException if email already exists
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDisplayName(request.getDisplayName());

        user = userRepository.save(user);
        log.info("User registered: {}", user.getEmail());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getDisplayName());
    }

    /**
     * Login an existing user.
     * @throws IllegalArgumentException if credentials are invalid
     */
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        log.info("User logged in: {}", user.getEmail());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getEmail(), user.getDisplayName());
    }
}
