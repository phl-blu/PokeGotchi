package com.tamagotchi.committracker.github;

import java.time.Duration;

/**
 * Defines polling modes for GitHub API requests based on rate limit status.
 * 
 * Requirements: 5.2, 5.3, 5.5
 */
public enum PollingMode {
    /**
     * Normal mode - plenty of requests available.
     * 5 minute polling interval.
     */
    NORMAL(Duration.ofMinutes(5)),
    
    /**
     * Conservative mode - low on requests (below 500).
     * 15 minute polling interval.
     */
    CONSERVATIVE(Duration.ofMinutes(15)),
    
    /**
     * Minimal mode - very low on requests (below 100).
     * 30 minute polling interval.
     */
    MINIMAL(Duration.ofMinutes(30)),
    
    /**
     * Paused mode - rate limit exhausted.
     * Wait for reset (1 hour max).
     */
    PAUSED(Duration.ofHours(1));
    
    private final Duration interval;
    
    PollingMode(Duration interval) {
        this.interval = interval;
    }
    
    /**
     * Gets the polling interval for this mode.
     * @return the duration between polls
     */
    public Duration getInterval() {
        return interval;
    }
}
