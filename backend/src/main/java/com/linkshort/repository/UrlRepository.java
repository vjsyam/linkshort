package com.linkshort.repository;

import com.linkshort.model.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying
    @Query("UPDATE UrlMapping u SET u.clickCount = u.clickCount + 1 WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode);

    List<UrlMapping> findAllByOrderByCreatedAtDesc();

    // v2: find URLs owned by a specific user
    List<UrlMapping> findByUserIdOrderByCreatedAtDesc(Long userId);

    // v2: count URLs by user
    long countByUserId(Long userId);
}
