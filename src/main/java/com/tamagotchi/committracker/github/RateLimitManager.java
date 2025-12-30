package com.tamagotchi.committracker.github;

import java.time.Duration;
import java.time.Instant;

/**
 * Interface for managing GitHub API rate limiting.
 * Tracks remaining requests and provides adaptive polling intervals.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.5, 5.6
 */
public interface RateLimitManager {
    
    /**
     * Checks if a request can be made within rate limits.
     * Returns true if request is allowed, false if should wait.
     * 
     * Requirement 5.6: Reserve 500 requests for user-initiated actions.
     * 
     * @return true if request is allowed
     */
    boolean canMakeRequest();
    
    /**
     * Records a request and updates rate limit tracking from GitHub headers.
     * 
     * Requirement 5.1: Track remaining requests using X-RateLimit headers.
     * 
     * @param remaining Remaining requests from X-RateLimit-Remaining header
     * @param resetTime Reset timestamp from X-RateLimit-Reset header
     */
    void recordRequest(int remaining, Instant resetTime);
    
    /**
     * Gets the recommended wait time before next request.
     * Returns Duration.ZERO if no wait needed.
     * 
     * @return duration to wait before next request
     */
    Duration getRecommendedWaitTime();
    
    /**
     * Gets current polling interval based on rate limit status.
     * 
     * Requirement 5.2: Switch to conservative polling when below 100 requests.
     * 
     * @return the current polling interval
     */
    Duration getCurrentPollingInterval();
    
    /**
     * Gets the current polling mode.
     * 
     * @return the current PollingMode
     */
    PollingMode getCurrentPollingMode();
    
    /**
     * Checks if we're in conservative mode (low on requests).
     * 
     * @return true if in conservative, minimal, or paused mode
     */
    boolean isConservativeMode();
    
    /**
     * Gets the current rate limit status.
     * 
     * @return the current RateLimitStatus
     */
    RateLimitStatus getRateLimitStatus();
    
    /**
     * Resets the rate limit tracking (e.g., after rate limit window resets).
     */
    void reset();
}
