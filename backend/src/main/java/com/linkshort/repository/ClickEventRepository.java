package com.linkshort.repository;

import com.linkshort.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for click event analytics data.
 *
 * Provides time-series aggregation queries for the analytics dashboard.
 */
@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    /**
     * Find all click events for a specific short code.
     */
    List<ClickEvent> findByShortCodeOrderByClickedAtDesc(String shortCode);

    /**
     * Count clicks for a short code within a date range.
     * Used for daily/hourly aggregation in analytics charts.
     */
    long countByShortCodeAndClickedAtBetween(
            String shortCode,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Get daily click counts for a short code over the last N days.
     * Returns pairs of (date_string, click_count) for chart rendering.
     *
     * NOTE: Uses CAST + FUNCTION for H2 compatibility.
     * For PostgreSQL, use: CAST(c.clickedAt AS DATE)
     */
    @Query("SELECT CAST(c.clickedAt AS DATE) as clickDate, COUNT(c) as clickCount " +
           "FROM ClickEvent c " +
           "WHERE c.shortCode = :shortCode " +
           "AND c.clickedAt >= :since " +
           "GROUP BY CAST(c.clickedAt AS DATE) " +
           "ORDER BY clickDate ASC")
    List<Object[]> getDailyClickCounts(
            @Param("shortCode") String shortCode,
            @Param("since") LocalDateTime since
    );
}
