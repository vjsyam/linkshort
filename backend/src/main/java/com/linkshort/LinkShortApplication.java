package com.linkshort;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * LinkShort — Production-Ready URL Shortener
 *
 * Entry point for the Spring Boot application.
 * @EnableAsync allows click tracking to run in a separate thread,
 * keeping redirects fast (non-blocking analytics).
 */
@SpringBootApplication
@EnableAsync
public class LinkShortApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinkShortApplication.class, args);
    }
}
