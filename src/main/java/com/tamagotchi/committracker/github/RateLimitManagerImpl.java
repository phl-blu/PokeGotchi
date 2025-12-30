package com.tamagotchi.committracker.github;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Implementation of RateLimitManager for GitHub API rate limiting.
 * 
 * Manages rate limit tracking and provides adaptive polling intervals
 * based on remaining API requests.
 * 
 * Requirements: 5.1, 5.2, 5.3, 5.5, 5.6
 */
public class RateLimitManagerImpl implements RateLimitManager {
    
    private static final Logger LOGGER = Logger.getLogger(RateLimitManagerImpl.class.getName());
    
    // Thresholds for mode switching (from design doc)
    private static final int NORMAL_THRESHOLD = 2000;
    private static final int CONSERVATIVE_THRESHOLD = 500;
    private static final int MINIMAL_THRESHOLD = 100;
    
    // Reserved buffer for user-initiated actions (Requirement 5.6)
    private static final int RESERVED_BUFFER = GitHubConfig.RATE_LIMIT_RESERVED_BUFFER;
    
    // Thread-safe storage for rate limit status
    private final AtomicReference<RateLimitStatus> currentStatus;
    
    /**
     * Creates a new RateLimitManagerImpl with default status.
     */
    public RateLimitManagerImpl() {
        this.currentStatus = new AtomicReference<>(RateLimitStatus.defaultStatus());
    }
    
    /**
     * Creates a new RateLimitManagerImpl with initial status.
     * 
     * @param initialStatus the initial rate limit status
     */
    public RateLimitManagerImpl(RateLimitStatus initialStatus) {
        this.currentStatus = new AtomicReference<>(
            initialStatus != null ? initialStatus : RateLimitStatus.defaultStatus()
        );
    }
    
    @Override
    public boolean canMakeRequest() {
        RateLimitStatus status = currentStatus.get();
        
        // Check if rate limit is exhausted
        if (status.isExhausted()) {
            LOGGER.warning("Rate limit exhausted. Reset at: " + status.resetTime());
            return false;
        }
        
        // Check if we're past the reset time (rate limit should have reset)
        if (Instant.now().isAfter(status.resetTime())) {
            // Rate limit window has reset, allow request
            return true;
        }
        
        // Reserve buffer for user-initiated actions (Requirement 5.6)
        // Only block automated requests when below reserved buffer
        if (status.remaining() <= RESERVED_BUFFER) {
            LOGGER.info("Rate limit below reserved buffer (" + RESERVED_BUFFER + 
                       "). Remaining: " + status.remaining());
            return false;
        }
        
        return true;
    }

    @Override
    public void recordRequest(int remaining, Instant resetTime) {
        if (remaining < 0) {
            throw new IllegalArgumentException("Remaining count cannot be negative");
        }
        if (resetTime == null) {
            throw new IllegalArgumentException("Reset time cannot be null");
        }
        
        RateLimitStatus oldStatus = currentStatus.get();
        int limit = oldStatus.limit();
        
        // Create new status with updated values
        RateLimitStatus newStatus = new RateLimitStatus(
            limit,
            remaining,
            resetTime,
            limit - remaining
        );
        
        currentStatus.set(newStatus);
        
        // Log mode changes
        PollingMode oldMode = determinePollingMode(oldStatus.remaining());
        PollingMode newMode = determinePollingMode(remaining);
        
        if (oldMode != newMode) {
            LOGGER.info("Polling mode changed from " + oldMode + " to " + newMode + 
                       ". Remaining requests: " + remaining);
        }
    }
    
    @Override
    public Duration getRecommendedWaitTime() {
        RateLimitStatus status = currentStatus.get();
        
        // If rate limit is exhausted, wait until reset
        if (status.isExhausted()) {
            Duration waitTime = Duration.between(Instant.now(), status.resetTime());
            // Return at least 1 second, at most the time until reset
            return waitTime.isNegative() ? Duration.ZERO : waitTime;
        }
        
        // If past reset time, no wait needed
        if (Instant.now().isAfter(status.resetTime())) {
            return Duration.ZERO;
        }
        
        // If below reserved buffer, wait until reset
        if (status.remaining() <= RESERVED_BUFFER) {
            Duration waitTime = Duration.between(Instant.now(), status.resetTime());
            return waitTime.isNegative() ? Duration.ZERO : waitTime;
        }
        
        // No wait needed
        return Duration.ZERO;
    }
    
    @Override
    public Duration getCurrentPollingInterval() {
        return getCurrentPollingMode().getInterval();
    }
    
    @Override
    public PollingMode getCurrentPollingMode() {
        RateLimitStatus status = currentStatus.get();
        return determinePollingMode(status.remaining());
    }
    
    /**
     * Determines the polling mode based on remaining requests.
     * 
     * Thresholds from design doc:
     * - Remaining > 2000: Normal mode (5 min polling)
     * - Remaining > 500:  Conservative mode (15 min polling)
     * - Remaining > 100:  Minimal mode (30 min polling)
     * - Remaining <= 100: Paused mode (wait for reset)
     * 
     * @param remaining the number of remaining requests
     * @return the appropriate PollingMode
     */
    private PollingMode determinePollingMode(int remaining) {
        if (remaining > NORMAL_THRESHOLD) {
            return PollingMode.NORMAL;
        } else if (remaining > CONSERVATIVE_THRESHOLD) {
            return PollingMode.CONSERVATIVE;
        } else if (remaining > MINIMAL_THRESHOLD) {
            return PollingMode.MINIMAL;
        } else {
            return PollingMode.PAUSED;
        }
    }
    
    @Override
    public boolean isConservativeMode() {
        PollingMode mode = getCurrentPollingMode();
        return mode != PollingMode.NORMAL;
    }
    
    @Override
    public RateLimitStatus getRateLimitStatus() {
        return currentStatus.get();
    }
    
    @Override
    public void reset() {
        currentStatus.set(RateLimitStatus.defaultStatus());
        LOGGER.info("Rate limit tracking reset to default values");
    }
}
