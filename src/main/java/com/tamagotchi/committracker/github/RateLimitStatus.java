package com.tamagotchi.committracker.github;

import java.time.Instant;

/**
 * Represents the current rate limit status from GitHub API.
 * 
 * Requirements: 5.1, 5.6
 */
public record RateLimitStatus(
    int limit,           // Total limit (5000 for authenticated)
    int remaining,       // Remaining requests
    Instant resetTime,   // When limit resets
    int used            // Requests used in current window
) {
    /**
     * Creates a RateLimitStatus with calculated used count.
     */
    public static RateLimitStatus of(int limit, int remaining, Instant resetTime) {
        return new RateLimitStatus(limit, remaining, resetTime, limit - remaining);
    }
    
    /**
     * Creates a default status for when no rate limit info is available.
     */
    public static RateLimitStatus defaultStatus() {
        return new RateLimitStatus(
            GitHubConfig.RATE_LIMIT_TOTAL,
            GitHubConfig.RATE_LIMIT_TOTAL,
            Instant.now().plusSeconds(3600),
            0
        );
    }
    
    /**
     * Checks if rate limit is exhausted.
     */
    public boolean isExhausted() {
        return remaining <= 0;
    }
    
    /**
     * Checks if we're below the conservative threshold.
     */
    public boolean isBelowConservativeThreshold() {
        return remaining <= GitHubConfig.RATE_LIMIT_CONSERVATIVE_THRESHOLD;
    }
    
    /**
     * Checks if we're below the minimal threshold.
     */
    public boolean isBelowMinimalThreshold() {
        return remaining <= GitHubConfig.RATE_LIMIT_MINIMAL_THRESHOLD;
    }
}
