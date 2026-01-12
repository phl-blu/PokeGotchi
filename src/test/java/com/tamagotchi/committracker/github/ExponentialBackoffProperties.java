package com.tamagotchi.committracker.github;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.DoubleRange;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Property-based tests for ExponentialBackoff correctness.
 * 
 * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
 * **Validates: Requirements 6.5, 8.1**
 * 
 * For any sequence of consecutive failures, the wait time between retries
 * SHALL increase exponentially up to a maximum cap.
 */
class ExponentialBackoffProperties {

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * For any sequence of consecutive failures, delays should increase
     * exponentially (each delay >= previous delay).
     */
    @Property(tries = 100)
    void delaysIncreaseExponentially(
            @ForAll @IntRange(min = 0, max = 20) int numAttempts) {
        
        // Use default configuration (1s base, 5min max, 10% jitter)
        ExponentialBackoff backoff = new ExponentialBackoff();
        
        List<Duration> delays = new ArrayList<>();
        for (int i = 0; i < numAttempts; i++) {
            delays.add(backoff.calculateDelayWithoutJitter(i));
        }
        
        // Verify delays are non-decreasing (exponential property)
        for (int i = 1; i < delays.size(); i++) {
            assertTrue(delays.get(i).compareTo(delays.get(i - 1)) >= 0,
                    "Delay at attempt " + i + " (" + delays.get(i).toMillis() + "ms) " +
                    "should be >= delay at attempt " + (i - 1) + " (" + delays.get(i - 1).toMillis() + "ms)");
        }
    }


    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * For any configuration, delays should never exceed the maximum cap.
     */
    @Property(tries = 100)
    void delaysNeverExceedMaximum(
            @ForAll @IntRange(min = 0, max = 100) int attemptNumber,
            @ForAll @LongRange(min = 100, max = 5000) long baseDelayMs,
            @ForAll @LongRange(min = 10000, max = 600000) long maxDelayMs) {
        
        // Ensure maxDelay >= baseDelay
        long actualMaxDelay = Math.max(maxDelayMs, baseDelayMs);
        
        ExponentialBackoff backoff = ExponentialBackoff.builder()
                .baseDelay(Duration.ofMillis(baseDelayMs))
                .maxDelay(Duration.ofMillis(actualMaxDelay))
                .jitterFactor(0.1)
                .build();
        
        Duration delay = backoff.calculateDelay(attemptNumber);
        
        assertTrue(delay.toMillis() <= actualMaxDelay,
                "Delay (" + delay.toMillis() + "ms) should not exceed max delay (" + actualMaxDelay + "ms)");
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * For any configuration, delays should always be positive.
     */
    @Property(tries = 100)
    void delaysAreAlwaysPositive(
            @ForAll @IntRange(min = 0, max = 50) int attemptNumber,
            @ForAll @LongRange(min = 1, max = 10000) long baseDelayMs,
            @ForAll @LongRange(min = 1000, max = 300000) long maxDelayMs,
            @ForAll @DoubleRange(min = 0.0, max = 1.0) double jitterFactor) {
        
        long actualMaxDelay = Math.max(maxDelayMs, baseDelayMs);
        
        ExponentialBackoff backoff = ExponentialBackoff.builder()
                .baseDelay(Duration.ofMillis(baseDelayMs))
                .maxDelay(Duration.ofMillis(actualMaxDelay))
                .jitterFactor(jitterFactor)
                .build();
        
        Duration delay = backoff.calculateDelay(attemptNumber);
        
        assertTrue(delay.toMillis() > 0,
                "Delay should always be positive, got: " + delay.toMillis() + "ms");
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * For attempt 0, the delay (without jitter) should equal the base delay.
     */
    @Property(tries = 100)
    void firstDelayEqualsBaseDelay(
            @ForAll @LongRange(min = 100, max = 10000) long baseDelayMs,
            @ForAll @LongRange(min = 10000, max = 300000) long maxDelayMs) {
        
        long actualMaxDelay = Math.max(maxDelayMs, baseDelayMs);
        
        ExponentialBackoff backoff = ExponentialBackoff.builder()
                .baseDelay(Duration.ofMillis(baseDelayMs))
                .maxDelay(Duration.ofMillis(actualMaxDelay))
                .jitterFactor(0.0) // No jitter for exact comparison
                .build();
        
        Duration delay = backoff.calculateDelay(0);
        
        assertEquals(baseDelayMs, delay.toMillis(),
                "First delay should equal base delay");
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * For any attempt, the delay without jitter should follow the formula:
     * min(baseDelay * 2^attempt, maxDelay)
     */
    @Property(tries = 100)
    void delayFollowsExponentialFormula(
            @ForAll @IntRange(min = 0, max = 10) int attemptNumber,
            @ForAll @LongRange(min = 100, max = 5000) long baseDelayMs,
            @ForAll @LongRange(min = 10000, max = 300000) long maxDelayMs) {
        
        long actualMaxDelay = Math.max(maxDelayMs, baseDelayMs);
        
        ExponentialBackoff backoff = ExponentialBackoff.builder()
                .baseDelay(Duration.ofMillis(baseDelayMs))
                .maxDelay(Duration.ofMillis(actualMaxDelay))
                .jitterFactor(0.0) // No jitter for exact comparison
                .build();
        
        Duration delay = backoff.calculateDelayWithoutJitter(attemptNumber);
        
        // Calculate expected delay: baseDelay * 2^attempt, capped at maxDelay
        long expectedDelay = Math.min(baseDelayMs * (1L << attemptNumber), actualMaxDelay);
        
        assertEquals(expectedDelay, delay.toMillis(),
                "Delay should follow exponential formula: base * 2^attempt, capped at max");
    }


    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * Jitter should keep delays within expected bounds:
     * delay * (1 - jitterFactor) <= jitteredDelay <= delay * (1 + jitterFactor)
     */
    @Property(tries = 100)
    void jitterStaysWithinBounds(
            @ForAll @IntRange(min = 0, max = 10) int attemptNumber,
            @ForAll @LongRange(min = 1000, max = 10000) long baseDelayMs,
            @ForAll @LongRange(min = 50000, max = 300000) long maxDelayMs,
            @ForAll @DoubleRange(min = 0.05, max = 0.5) double jitterFactor) {
        
        long actualMaxDelay = Math.max(maxDelayMs, baseDelayMs);
        
        // Use a fixed seed for reproducibility
        Random fixedRandom = new Random(42);
        ExponentialBackoff backoff = new ExponentialBackoff(
                Duration.ofMillis(baseDelayMs),
                Duration.ofMillis(actualMaxDelay),
                jitterFactor,
                fixedRandom
        );
        
        Duration baseDelay = backoff.calculateDelayWithoutJitter(attemptNumber);
        
        // Run multiple times to test jitter bounds
        for (int i = 0; i < 10; i++) {
            Duration jitteredDelay = backoff.calculateDelay(attemptNumber);
            
            long minExpected = (long) (baseDelay.toMillis() * (1 - jitterFactor));
            long maxExpected = Math.min(
                    (long) (baseDelay.toMillis() * (1 + jitterFactor)),
                    actualMaxDelay
            );
            
            // Allow for minimum of 1ms
            minExpected = Math.max(1, minExpected);
            
            assertTrue(jitteredDelay.toMillis() >= minExpected,
                    "Jittered delay (" + jitteredDelay.toMillis() + "ms) should be >= " + minExpected + "ms");
            assertTrue(jitteredDelay.toMillis() <= maxExpected,
                    "Jittered delay (" + jitteredDelay.toMillis() + "ms) should be <= " + maxExpected + "ms");
        }
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * Reset should set attempt counter back to zero.
     */
    @Property(tries = 100)
    void resetClearsAttemptCounter(
            @ForAll @IntRange(min = 1, max = 20) int numAttempts) {
        
        ExponentialBackoff backoff = new ExponentialBackoff();
        
        // Make several attempts
        for (int i = 0; i < numAttempts; i++) {
            backoff.nextDelay();
        }
        
        assertEquals(numAttempts, backoff.getAttemptCount(),
                "Attempt count should match number of nextDelay() calls");
        
        // Reset
        backoff.reset();
        
        assertEquals(0, backoff.getAttemptCount(),
                "Attempt count should be 0 after reset");
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * nextDelay() should increment the attempt counter and return appropriate delay.
     */
    @Property(tries = 100)
    void nextDelayIncrementsCounter(
            @ForAll @IntRange(min = 1, max = 10) int numCalls) {
        
        ExponentialBackoff backoff = ExponentialBackoff.builder()
                .baseDelay(Duration.ofMillis(100))
                .maxDelay(Duration.ofMinutes(5))
                .jitterFactor(0.0)
                .build();
        
        List<Duration> delays = new ArrayList<>();
        
        for (int i = 0; i < numCalls; i++) {
            assertEquals(i, backoff.getAttemptCount(),
                    "Attempt count should be " + i + " before call " + (i + 1));
            
            Duration delay = backoff.nextDelay();
            delays.add(delay);
            
            assertEquals(i + 1, backoff.getAttemptCount(),
                    "Attempt count should be " + (i + 1) + " after call " + (i + 1));
        }
        
        // Verify delays are non-decreasing
        for (int i = 1; i < delays.size(); i++) {
            assertTrue(delays.get(i).compareTo(delays.get(i - 1)) >= 0,
                    "Delays should be non-decreasing");
        }
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * Default configuration should use 1s base delay and 5min max delay.
     */
    @Property(tries = 10)
    void defaultConfigurationIsCorrect() {
        ExponentialBackoff backoff = new ExponentialBackoff();
        
        assertEquals(Duration.ofSeconds(1), backoff.getBaseDelay(),
                "Default base delay should be 1 second");
        assertEquals(Duration.ofMinutes(5), backoff.getMaxDelay(),
                "Default max delay should be 5 minutes");
        assertEquals(0.1, backoff.getJitterFactor(), 0.001,
                "Default jitter factor should be 0.1 (10%)");
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * For very large attempt numbers, delay should be capped at max delay.
     */
    @Property(tries = 100)
    void largeAttemptNumbersCappedAtMax(
            @ForAll @IntRange(min = 50, max = 1000) int attemptNumber) {
        
        ExponentialBackoff backoff = new ExponentialBackoff();
        
        Duration delay = backoff.calculateDelayWithoutJitter(attemptNumber);
        
        assertEquals(Duration.ofMinutes(5), delay,
                "Large attempt numbers should be capped at max delay (5 minutes)");
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * Invalid configurations should throw IllegalArgumentException.
     */
    @Property(tries = 10)
    void invalidConfigurationThrowsException() {
        // Negative base delay
        assertThrows(IllegalArgumentException.class, () ->
                ExponentialBackoff.builder()
                        .baseDelay(Duration.ofMillis(-100))
                        .maxDelay(Duration.ofMinutes(5))
                        .build());
        
        // Zero base delay
        assertThrows(IllegalArgumentException.class, () ->
                ExponentialBackoff.builder()
                        .baseDelay(Duration.ZERO)
                        .maxDelay(Duration.ofMinutes(5))
                        .build());
        
        // Max delay less than base delay
        assertThrows(IllegalArgumentException.class, () ->
                ExponentialBackoff.builder()
                        .baseDelay(Duration.ofSeconds(10))
                        .maxDelay(Duration.ofSeconds(5))
                        .build());
        
        // Invalid jitter factor
        assertThrows(IllegalArgumentException.class, () ->
                ExponentialBackoff.builder()
                        .baseDelay(Duration.ofSeconds(1))
                        .maxDelay(Duration.ofMinutes(5))
                        .jitterFactor(-0.1)
                        .build());
        
        assertThrows(IllegalArgumentException.class, () ->
                ExponentialBackoff.builder()
                        .baseDelay(Duration.ofSeconds(1))
                        .maxDelay(Duration.ofMinutes(5))
                        .jitterFactor(1.5)
                        .build());
    }

    /**
     * **Feature: github-api-integration, Property 6: Exponential Backoff Correctness**
     * **Validates: Requirements 6.5, 8.1**
     * 
     * Negative attempt numbers should throw IllegalArgumentException.
     */
    @Property(tries = 100)
    void negativeAttemptNumberThrowsException(
            @ForAll @IntRange(min = -1000, max = -1) int negativeAttempt) {
        
        ExponentialBackoff backoff = new ExponentialBackoff();
        
        assertThrows(IllegalArgumentException.class, () ->
                backoff.calculateDelay(negativeAttempt));
    }
}
