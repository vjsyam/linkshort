package com.linkshort.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for non-blocking click tracking.
 *
 * WHY ASYNC?
 * - Redirects (GET /{shortCode}) must be FAST — <5ms ideally
 * - Click tracking involves DB writes (INSERT into click_events + UPDATE click_count)
 * - By making tracking async, the user gets their redirect immediately
 *   while analytics data is recorded in the background
 *
 * THREAD POOL SIZING:
 * - Core: 5 threads (handles baseline analytics load)
 * - Max: 10 threads (handles traffic spikes)
 * - Queue: 500 (buffers burst traffic before rejecting)
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("click-tracker-");
        executor.initialize();
        return executor;
    }
}
