package com.tamagotchi.committracker.github;

import java.time.Duration;
import java.util.Random;

/**
 * Implements exponential backoff with jitter for retry logic.
 * Used for handling transient failures in GitHub API requests.
 * 
 * Requirements: 6.5, 8.1
 * - Base delay: 1 second
 * - Max delay: 5 minutes
 * - Jitter factor: 10% to prevent thundering herd
 */
public class ExponentialBackoff {
    
    private static final Duration BASE_DELAY = Duration.ofSeconds(1);
    private static final Duration MAX_DELAY = Duration.ofMinutes(5);
    private static final double JITTER_FACTOR = 0.1;
    
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final double jitterFactor;
    private final Random random;
    
    private int attemptCount;
    
    /**
     * Creates an ExponentialBackoff with default configuration.
     * Base delay: 1s, Max delay: 5min, Jitter: 10%
     */
    public ExponentialBackoff() {
        this(BASE_DELAY, MAX_DELAY, JITTER_FACTOR);
    }
    
    /**
     * Creates an ExponentialBackoff with custom configuration.
     * 
     * @param baseDelay Initial delay before first retry
     * @param maxDelay Maximum delay cap
     * @param jitterFactor Jitter factor (0.0 to 1.0) for randomization
     */
    public ExponentialBackoff(Duration baseDelay, Duration maxDelay, double jitterFactor) {
        this(baseDelay, maxDelay, jitterFactor, new Random());
    }
    
    /**
     * Creates an ExponentialBackoff with custom configuration and random source.
     * Useful for testing with deterministic random values.
     */
    ExponentialBackoff(Duration baseDelay, Duration maxDelay, double jitterFactor, Random random) {
        if (baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("Base delay must be positive");
        }
        if (maxDelay == null || maxDelay.isNegative() || maxDelay.isZero()) {
            throw new IllegalArgumentException("Max delay must be positive");
        }
        if (maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("Max delay must be >= base delay");
        }
        if (jitterFactor < 0.0 || jitterFactor > 1.0) {
            throw new IllegalArgumentException("Jitter factor must be between 0.0 and 1.0");
        }
        
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
        this.jitterFactor = jitterFactor;
        this.random = random;
        this.attemptCount = 0;
    }

    
    /**
     * Calculates the delay for a given attempt number.
     * The delay increases exponentially: baseDelay * 2^attemptNumber
     * Jitter is applied to prevent thundering herd problem.
     * 
     * @param attemptNumber The attempt number (0-based)
     * @return The calculated delay with jitter applied
     */
    public Duration calculateDelay(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("Attempt number must be non-negative");
        }
        
        // Calculate base exponential delay: baseDelay * 2^attemptNumber
        long baseDelayMs = baseDelay.toMillis();
        long exponentialDelayMs;
        
        // Prevent overflow for large attempt numbers
        if (attemptNumber >= 63) {
            exponentialDelayMs = maxDelay.toMillis();
        } else {
            long multiplier = 1L << attemptNumber; // 2^attemptNumber
            // Check for overflow
            if (multiplier > Long.MAX_VALUE / baseDelayMs) {
                exponentialDelayMs = maxDelay.toMillis();
            } else {
                exponentialDelayMs = baseDelayMs * multiplier;
            }
        }
        
        // Cap at max delay
        long cappedDelayMs = Math.min(exponentialDelayMs, maxDelay.toMillis());
        
        // Apply jitter: delay * (1 + random(-jitterFactor, +jitterFactor))
        double jitter = (random.nextDouble() - 0.5) * 2 * jitterFactor;
        long jitteredDelayMs = (long) (cappedDelayMs * (1 + jitter));
        
        // Ensure delay is at least 1ms and doesn't exceed max
        jitteredDelayMs = Math.max(1, jitteredDelayMs);
        jitteredDelayMs = Math.min(jitteredDelayMs, maxDelay.toMillis());
        
        return Duration.ofMillis(jitteredDelayMs);
    }
    
    /**
     * Calculates the delay without jitter for testing/verification.
     * 
     * @param attemptNumber The attempt number (0-based)
     * @return The calculated delay without jitter
     */
    public Duration calculateDelayWithoutJitter(int attemptNumber) {
        if (attemptNumber < 0) {
            throw new IllegalArgumentException("Attempt number must be non-negative");
        }
        
        long baseDelayMs = baseDelay.toMillis();
        long exponentialDelayMs;
        
        if (attemptNumber >= 63) {
            exponentialDelayMs = maxDelay.toMillis();
        } else {
            long multiplier = 1L << attemptNumber;
            if (multiplier > Long.MAX_VALUE / baseDelayMs) {
                exponentialDelayMs = maxDelay.toMillis();
            } else {
                exponentialDelayMs = baseDelayMs * multiplier;
            }
        }
        
        return Duration.ofMillis(Math.min(exponentialDelayMs, maxDelay.toMillis()));
    }
    
    /**
     * Gets the delay for the next retry attempt and increments the counter.
     * 
     * @return The delay for the next retry
     */
    public Duration nextDelay() {
        return calculateDelay(attemptCount++);
    }
    
    /**
     * Gets the current attempt count.
     * 
     * @return The number of attempts made
     */
    public int getAttemptCount() {
        return attemptCount;
    }
    
    /**
     * Resets the attempt counter to zero.
     * Call this after a successful operation.
     */
    public void reset() {
        attemptCount = 0;
    }
    
    /**
     * Checks if the backoff has reached the maximum delay.
     * 
     * @return true if the next delay would be at the maximum
     */
    public boolean isAtMaxDelay() {
        return calculateDelayWithoutJitter(attemptCount).compareTo(maxDelay) >= 0;
    }
    
    /**
     * Gets the base delay configuration.
     */
    public Duration getBaseDelay() {
        return baseDelay;
    }
    
    /**
     * Gets the max delay configuration.
     */
    public Duration getMaxDelay() {
        return maxDelay;
    }
    
    /**
     * Gets the jitter factor configuration.
     */
    public double getJitterFactor() {
        return jitterFactor;
    }
    
    /**
     * Creates a builder for custom ExponentialBackoff configuration.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for ExponentialBackoff configuration.
     */
    public static class Builder {
        private Duration baseDelay = BASE_DELAY;
        private Duration maxDelay = MAX_DELAY;
        private double jitterFactor = JITTER_FACTOR;
        
        public Builder baseDelay(Duration baseDelay) {
            this.baseDelay = baseDelay;
            return this;
        }
        
        public Builder maxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
            return this;
        }
        
        public Builder jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }
        
        public ExponentialBackoff build() {
            return new ExponentialBackoff(baseDelay, maxDelay, jitterFactor);
        }
    }
}
