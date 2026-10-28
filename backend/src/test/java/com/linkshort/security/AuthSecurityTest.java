package com.linkshort.security;

import com.linkshort.repository.UserRepository;
import com.linkshort.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @Test
    @DisplayName("BCrypt hashes match the raw password and never equal it")
    void bcryptHashing() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("S3cret!pass");

        assertNotEquals("S3cret!pass", hash);
        assertTrue(encoder.matches("S3cret!pass", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    @DisplayName("registration stores a hash, not the plain password")
    void registrationStoresHash() throws Exception {
        String body = "{\"email\":\"test1@example.com\",\"password\":\"S3cret!pass\",\"displayName\":\"Test User 1\"}";

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated());

        var saved = users.findByEmail("test1@example.com").orElseThrow();
        assertNotEquals("S3cret!pass", saved.getPassword());
        assertTrue(passwordEncoder.matches("S3cret!pass", saved.getPassword()));
    }

    @Test
    @DisplayName("login with a wrong password is rejected")
    void wrongPasswordRejected() throws Exception {
        String reg = "{\"email\":\"test2@example.com\",\"password\":\"S3cret!pass\",\"displayName\":\"Test User 2\"}";
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(reg))
           .andExpect(status().isCreated());

        String bad = "{\"email\":\"test2@example.com\",\"password\":\"wrongpassword\"}";
        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(bad))
           .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a protected endpoint refuses requests with no token")
    void protectedEndpointNeedsToken() throws Exception {
        mvc.perform(get("/api/urls/my"))
           .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a tampered token is refused")
    void tamperedTokenRefused() throws Exception {
        mvc.perform(get("/api/urls/my")
                .header("Authorization", "Bearer not.a.real.jwt.token"))
           .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("a valid token allows access to protected endpoints")
    void validTokenAllowed() throws Exception {
        String reg = "{\"email\":\"test3@example.com\",\"password\":\"S3cret!pass\",\"displayName\":\"Test User 3\"}";
        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(reg))
           .andExpect(status().isCreated());

        var user = users.findByEmail("test3@example.com").orElseThrow();
        String validToken = jwtService.generateToken(user.getId(), user.getEmail());

        mvc.perform(get("/api/urls/my")
                .header("Authorization", "Bearer " + validToken))
           .andExpect(status().isOk());
    }
}
