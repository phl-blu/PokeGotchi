package com.tamagotchi.committracker.github;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for RateLimitManager.
 * 
 * These tests validate the correctness properties defined in the design document
 * for rate limit tracking and adaptive polling intervals.
 */
class RateLimitManagerProperties {

    /**
     * **Feature: github-api-integration, Property 2: Rate Limit Tracking Consistency**
     * **Validates: Requirements 5.1, 5.2**
     * 
     * For any sequence of API requests, the rate limit manager's tracked remaining count 
     * SHALL never exceed the actual remaining count reported by GitHub headers.
     * 
     * This property ensures that the manager always reflects the most recent
     * rate limit information from GitHub and never overestimates available requests.
     */
    @Property(tries = 100)
    void rateLimitTrackingConsistency(
            @ForAll("rateLimitSequences") List<Integer> remainingSequence) {
        
        RateLimitManagerImpl manager = new RateLimitManagerImpl();
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        for (int reportedRemaining : remainingSequence) {
            // Record the request with the reported remaining count
            manager.recordRequest(reportedRemaining, resetTime);
            
            // Get the tracked status
            RateLimitStatus trackedStatus = manager.getRateLimitStatus();
            
            // Property: tracked remaining SHALL never exceed reported remaining
            assertTrue(trackedStatus.remaining() <= reportedRemaining,
                    String.format("Tracked remaining (%d) exceeds reported remaining (%d)",
                            trackedStatus.remaining(), reportedRemaining));
            
            // After recording, tracked should equal reported (exact tracking)
            assertEquals(reportedRemaining, trackedStatus.remaining(),
                    String.format("Tracked remaining (%d) should equal reported remaining (%d) after recording",
                            trackedStatus.remaining(), reportedRemaining));
        }
    }

    /**
     * **Feature: github-api-integration, Property 3: Polling Interval Monotonicity**
     * **Validates: Requirements 5.2, 5.3, 6.3, 6.4**
     * 
     * For any decreasing rate limit remaining count, the polling interval SHALL be 
     * greater than or equal to the previous interval (never decrease when rate limit decreases).
     * 
     * This property ensures that as rate limits decrease, polling becomes less frequent
     * to conserve remaining requests.
     */
    @Property(tries = 100)
    void pollingIntervalMonotonicity(
            @ForAll("decreasingRateLimitSequences") List<Integer> decreasingSequence) {
        
        RateLimitManagerImpl manager = new RateLimitManagerImpl();
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        Duration previousInterval = Duration.ZERO;
        
        for (int remaining : decreasingSequence) {
            // Record the request with decreasing remaining count
            manager.recordRequest(remaining, resetTime);
            
            // Get the current polling interval
            Duration currentInterval = manager.getCurrentPollingInterval();
            
            // Property: interval should never decrease as remaining decreases
            assertTrue(currentInterval.compareTo(previousInterval) >= 0,
                    String.format("Polling interval decreased from %s to %s when remaining dropped to %d",
                            previousInterval, currentInterval, remaining));
            
            previousInterval = currentInterval;
        }
    }

    /**
     * **Feature: github-api-integration, Property 2: Rate Limit Tracking Consistency (Boundary)**
     * **Validates: Requirements 5.1, 5.2**
     * 
     * For any valid remaining count at mode boundaries, the manager SHALL correctly
     * track the remaining count and select the appropriate polling mode.
     */
    @Property(tries = 100)
    void rateLimitTrackingAtBoundaries(
            @ForAll("boundaryRemainingValues") int remaining) {
        
        RateLimitManagerImpl manager = new RateLimitManagerImpl();
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        // Record the request
        manager.recordRequest(remaining, resetTime);
        
        // Verify tracking consistency
        RateLimitStatus status = manager.getRateLimitStatus();
        assertEquals(remaining, status.remaining(),
                "Tracked remaining should match reported remaining at boundaries");
        
        // Verify correct mode selection based on thresholds
        PollingMode mode = manager.getCurrentPollingMode();
        PollingMode expectedMode = determineExpectedMode(remaining);
        
        assertEquals(expectedMode, mode,
                String.format("Expected mode %s for remaining=%d, but got %s",
                        expectedMode, remaining, mode));
    }

    /**
     * **Feature: github-api-integration, Property 3: Polling Interval Monotonicity (Mode Transitions)**
     * **Validates: Requirements 5.2, 5.3, 6.3, 6.4**
     * 
     * For any transition between polling modes, the interval SHALL increase or stay the same
     * when transitioning to a more conservative mode.
     */
    @Property(tries = 100)
    void pollingModeTransitionsAreMonotonic(
            @ForAll("modeTransitionPairs") ModeTransitionPair transition) {
        
        RateLimitManagerImpl manager = new RateLimitManagerImpl();
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        // Set initial state
        manager.recordRequest(transition.fromRemaining, resetTime);
        Duration initialInterval = manager.getCurrentPollingInterval();
        PollingMode initialMode = manager.getCurrentPollingMode();
        
        // Transition to new state (lower remaining)
        manager.recordRequest(transition.toRemaining, resetTime);
        Duration newInterval = manager.getCurrentPollingInterval();
        PollingMode newMode = manager.getCurrentPollingMode();
        
        // If remaining decreased, interval should not decrease
        if (transition.toRemaining < transition.fromRemaining) {
            assertTrue(newInterval.compareTo(initialInterval) >= 0,
                    String.format("Interval decreased from %s (%s) to %s (%s) when remaining dropped from %d to %d",
                            initialInterval, initialMode, newInterval, newMode,
                            transition.fromRemaining, transition.toRemaining));
        }
    }

    /**
     * Property: Conservative mode detection is consistent with polling mode.
     * isConservativeMode() should return true for all modes except NORMAL.
     */
    @Property(tries = 100)
    void conservativeModeConsistency(
            @ForAll @IntRange(min = 0, max = 5000) int remaining) {
        
        RateLimitManagerImpl manager = new RateLimitManagerImpl();
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        manager.recordRequest(remaining, resetTime);
        
        PollingMode mode = manager.getCurrentPollingMode();
        boolean isConservative = manager.isConservativeMode();
        
        // isConservativeMode should be true for all modes except NORMAL
        boolean expectedConservative = (mode != PollingMode.NORMAL);
        assertEquals(expectedConservative, isConservative,
                String.format("isConservativeMode() returned %b for mode %s (remaining=%d)",
                        isConservative, mode, remaining));
    }

    /**
     * Property: canMakeRequest respects rate limit exhaustion.
     * When remaining is 0, canMakeRequest should return false.
     */
    @Property(tries = 50)
    void canMakeRequestRespectsExhaustion(
            @ForAll @IntRange(min = 0, max = 100) int remaining) {
        
        RateLimitManagerImpl manager = new RateLimitManagerImpl();
        // Set reset time in the future so we're within the rate limit window
        Instant resetTime = Instant.now().plusSeconds(3600);
        
        manager.recordRequest(remaining, resetTime);
        
        boolean canMake = manager.canMakeRequest();
        
        // When remaining is 0, should not be able to make requests
        if (remaining == 0) {
            assertFalse(canMake,
                    "canMakeRequest should return false when remaining is 0");
        }
        
        // When remaining is above reserved buffer (500), should be able to make requests
        if (remaining > GitHubConfig.RATE_LIMIT_RESERVED_BUFFER) {
            assertTrue(canMake,
                    String.format("canMakeRequest should return true when remaining (%d) > reserved buffer (%d)",
                            remaining, GitHubConfig.RATE_LIMIT_RESERVED_BUFFER));
        }
    }

    // ==================== Arbitrary Providers ====================

    /**
     * Provides sequences of remaining counts simulating API responses.
     * Values can go up or down (simulating rate limit resets or consumption).
     */
    @Provide
    Arbitrary<List<Integer>> rateLimitSequences() {
        return Arbitraries.integers()
                .between(0, 5000)
                .list()
                .ofMinSize(1)
                .ofMaxSize(20);
    }

    /**
     * Provides strictly decreasing sequences of remaining counts.
     * Simulates continuous API consumption without resets.
     */
    @Provide
    Arbitrary<List<Integer>> decreasingRateLimitSequences() {
        return Arbitraries.integers()
                .between(100, 5000)
                .list()
                .ofMinSize(2)
                .ofMaxSize(15)
                .map(this::makeDecreasing);
    }

    /**
     * Provides remaining values at or near mode transition boundaries.
     */
    @Provide
    Arbitrary<Integer> boundaryRemainingValues() {
        return Arbitraries.oneOf(
                // Around NORMAL threshold (2000)
                Arbitraries.integers().between(1998, 2002),
                // Around CONSERVATIVE threshold (500)
                Arbitraries.integers().between(498, 502),
                // Around MINIMAL threshold (100)
                Arbitraries.integers().between(98, 102),
                // Edge cases
                Arbitraries.of(0, 1, 4999, 5000)
        );
    }

    /**
     * Provides pairs of remaining values for mode transition testing.
     */
    @Provide
    Arbitrary<ModeTransitionPair> modeTransitionPairs() {
        return Arbitraries.integers().between(0, 5000)
                .tuple2()
                .filter(t -> t.get1() > t.get2()) // Ensure fromRemaining > toRemaining
                .map(t -> new ModeTransitionPair(t.get1(), t.get2()));
    }

    // ==================== Helper Methods ====================

    /**
     * Converts a list to a strictly decreasing sequence.
     */
    private List<Integer> makeDecreasing(List<Integer> original) {
        if (original.isEmpty()) {
            return original;
        }
        
        // Sort in descending order to ensure decreasing sequence
        return original.stream()
                .sorted((a, b) -> b - a)
                .distinct()
                .toList();
    }

    /**
     * Determines the expected polling mode based on remaining count.
     * Mirrors the logic in RateLimitManagerImpl.
     */
    private PollingMode determineExpectedMode(int remaining) {
        if (remaining > 2000) {
            return PollingMode.NORMAL;
        } else if (remaining > 500) {
            return PollingMode.CONSERVATIVE;
        } else if (remaining > 100) {
            return PollingMode.MINIMAL;
        } else {
            return PollingMode.PAUSED;
        }
    }

    /**
     * Record class for mode transition testing.
     */
    record ModeTransitionPair(int fromRemaining, int toRemaining) {}
}
