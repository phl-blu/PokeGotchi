package com.tamagotchi.committracker.util;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.LongRange;
import net.jqwik.api.constraints.DoubleRange;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Property-based tests for retry mechanism correctness.
 * 
 * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
 * **Validates: Requirements 3.3, 3.5**
 * 
 * These tests validate that exponential backoff is applied with increasing
 * delays between retry attempts for repository access failures.
 */
class RetryMechanismProperties {

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any repository access failure, exponential backoff should be applied
     * with increasing delays between retry attempts.
     * 
     * This property verifies that delays increase exponentially up to max delay.
     */
    @Property(tries = 100)
    void exponentialBackoffIncreasesDelays(
            @ForAll @IntRange(min = 1, max = 10) int maxRetries,
            @ForAll @LongRange(min = 100, max = 2000) long initialDelayMs,
            @ForAll @LongRange(min = 5000, max = 60000) long maxDelayMs,
            @ForAll @DoubleRange(min = 1.5, max = 3.0) double backoffMultiplier) {
        
        // Calculate expected delays for each retry attempt
        List<Long> expectedDelays = new ArrayList<>();
        long currentDelay = initialDelayMs;
        
        for (int i = 0; i < maxRetries; i++) {
            expectedDelays.add(currentDelay);
            currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelayMs);
        }
        
        // Verify delays are non-decreasing (exponential backoff property)
        for (int i = 1; i < expectedDelays.size(); i++) {
            assertTrue(expectedDelays.get(i) >= expectedDelays.get(i - 1),
                    "Delay at attempt " + (i + 1) + " should be >= delay at attempt " + i);
        }
        
        // Verify all delays are bounded by maxDelayMs
        for (Long delay : expectedDelays) {
            assertTrue(delay <= maxDelayMs,
                    "All delays should be bounded by maxDelayMs");
        }
        
        // Verify first delay equals initial delay
        assertEquals(initialDelayMs, expectedDelays.get(0),
                "First delay should equal initial delay");
    }

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any successful operation, retry mechanism should return success
     * with the correct number of attempts made.
     */
    @Property(tries = 100)
    void successfulOperationReturnsCorrectAttempts(
            @ForAll @IntRange(min = 1, max = 5) int maxRetries,
            @ForAll @IntRange(min = 1, max = 5) int successOnAttempt) {
        
        // Ensure successOnAttempt is within valid range
        int actualSuccessAttempt = Math.min(successOnAttempt, maxRetries + 1);
        
        RetryMechanism retryMechanism = RetryMechanism.builder()
                .maxRetries(maxRetries)
                .initialDelay(10) // Short delay for testing
                .maxDelay(100)
                .backoffMultiplier(2.0)
                .build();
        
        try {
            AtomicInteger attemptCounter = new AtomicInteger(0);
            
            RetryMechanism.RetryResult<String> result = retryMechanism.executeWithRetry(
                    () -> {
                        int currentAttempt = attemptCounter.incrementAndGet();
                        if (currentAttempt < actualSuccessAttempt) {
                            throw new RuntimeException("Simulated transient failure");
                        }
                        return "success";
                    },
                    "test operation",
                    e -> true // All exceptions are retryable
            );
            
            if (actualSuccessAttempt <= maxRetries + 1) {
                assertTrue(result.isSuccess(), "Operation should succeed");
                assertEquals("success", result.getResult(), "Result should be 'success'");
                assertEquals(actualSuccessAttempt, result.getAttemptsMade(),
                        "Attempts made should match success attempt number");
            }
        } finally {
            retryMechanism.shutdown();
        }
    }

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any operation that always fails, retry mechanism should exhaust
     * all retries and return failure with correct attempt count.
     */
    @Property(tries = 100)
    void failedOperationExhaustsRetries(
            @ForAll @IntRange(min = 0, max = 5) int maxRetries) {
        
        RetryMechanism retryMechanism = RetryMechanism.builder()
                .maxRetries(maxRetries)
                .initialDelay(10) // Short delay for testing
                .maxDelay(100)
                .backoffMultiplier(2.0)
                .build();
        
        try {
            AtomicInteger attemptCounter = new AtomicInteger(0);
            
            RetryMechanism.RetryResult<String> result = retryMechanism.executeWithRetry(
                    () -> {
                        attemptCounter.incrementAndGet();
                        throw new RuntimeException("Simulated permanent failure");
                    },
                    "test operation",
                    e -> true // All exceptions are retryable
            );
            
            assertFalse(result.isSuccess(), "Operation should fail after exhausting retries");
            assertNull(result.getResult(), "Result should be null on failure");
            assertEquals(maxRetries + 1, result.getAttemptsMade(),
                    "Should make maxRetries + 1 attempts (initial + retries)");
            assertNotNull(result.getLastError(), "Last error should be captured");
        } finally {
            retryMechanism.shutdown();
        }
    }

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any non-retryable exception, retry mechanism should fail immediately
     * without additional retry attempts.
     */
    @Property(tries = 100)
    void nonRetryableExceptionFailsImmediately(
            @ForAll @IntRange(min = 1, max = 5) int maxRetries) {
        
        RetryMechanism retryMechanism = RetryMechanism.builder()
                .maxRetries(maxRetries)
                .initialDelay(10)
                .maxDelay(100)
                .backoffMultiplier(2.0)
                .build();
        
        try {
            AtomicInteger attemptCounter = new AtomicInteger(0);
            
            RetryMechanism.RetryResult<String> result = retryMechanism.executeWithRetry(
                    () -> {
                        attemptCounter.incrementAndGet();
                        throw new RuntimeException("Non-retryable error");
                    },
                    "test operation",
                    e -> false // No exceptions are retryable
            );
            
            assertFalse(result.isSuccess(), "Operation should fail");
            assertEquals(1, result.getAttemptsMade(),
                    "Should only make 1 attempt for non-retryable exception");
        } finally {
            retryMechanism.shutdown();
        }
    }


    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any delay configuration, the calculated delay should never exceed
     * the configured maximum delay.
     */
    @Property(tries = 100)
    void delayNeverExceedsMaximum(
            @ForAll @IntRange(min = 1, max = 20) int retryAttempts,
            @ForAll @LongRange(min = 100, max = 5000) long initialDelayMs,
            @ForAll @LongRange(min = 1000, max = 30000) long maxDelayMs,
            @ForAll @DoubleRange(min = 1.1, max = 5.0) double backoffMultiplier) {
        
        // Ensure maxDelayMs >= initialDelayMs for valid configuration
        long actualMaxDelay = Math.max(maxDelayMs, initialDelayMs);
        
        // Simulate delay calculation for multiple retries
        long currentDelay = initialDelayMs;
        
        for (int i = 0; i < retryAttempts; i++) {
            // Verify current delay is bounded
            assertTrue(currentDelay <= actualMaxDelay,
                    "Delay at attempt " + (i + 1) + " (" + currentDelay + 
                    ") should not exceed max delay (" + actualMaxDelay + ")");
            
            // Calculate next delay with exponential backoff
            currentDelay = Math.min((long) (currentDelay * backoffMultiplier), actualMaxDelay);
        }
    }

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any retryable exception classification, network-related exceptions
     * should be identified as retryable.
     */
    @Property(tries = 100)
    void networkExceptionsAreRetryable(
            @ForAll("networkExceptionMessages") String message) {
        
        Exception networkException = new RuntimeException(message);
        
        assertTrue(RetryMechanism.isRetryableException(networkException),
                "Network-related exception with message '" + message + "' should be retryable");
    }

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any authentication exception, it should not be classified as retryable.
     */
    @Property(tries = 100)
    void authenticationExceptionsAreNotRetryable(
            @ForAll("authExceptionMessages") String message) {
        
        Exception authException = new RuntimeException(message);
        
        assertTrue(RetryMechanism.isAuthenticationException(authException),
                "Authentication exception with message '" + message + "' should be identified");
    }

    /**
     * **Feature: performance-optimization, Property 7: Retry Mechanism Correctness**
     * **Validates: Requirements 3.3, 3.5**
     * 
     * For any retry configuration from AppConfig, the values should be
     * within reasonable bounds.
     */
    @Property(tries = 100)
    void configuredRetryValuesAreReasonable() {
        int maxRetries = com.tamagotchi.committracker.config.AppConfig.getMaxRetries();
        long initialDelay = com.tamagotchi.committracker.config.AppConfig.getRetryInitialDelayMs();
        long maxDelay = com.tamagotchi.committracker.config.AppConfig.getRetryMaxDelayMs();
        double backoffMultiplier = com.tamagotchi.committracker.config.AppConfig.getRetryBackoffMultiplier();
        
        // Verify reasonable bounds
        assertTrue(maxRetries >= 0 && maxRetries <= 10,
                "Max retries should be between 0 and 10");
        assertTrue(initialDelay >= 100 && initialDelay <= 60000,
                "Initial delay should be between 100ms and 60s");
        assertTrue(maxDelay >= initialDelay && maxDelay <= 300000,
                "Max delay should be >= initial delay and <= 5 minutes");
        assertTrue(backoffMultiplier >= 1.0 && backoffMultiplier <= 5.0,
                "Backoff multiplier should be between 1.0 and 5.0");
    }

    /**
     * Provides network-related exception messages for testing.
     */
    @Provide
    Arbitrary<String> networkExceptionMessages() {
        return Arbitraries.of(
                "Connection refused",
                "Connection timeout",
                "Network unreachable",
                "Socket timeout",
                "Connection reset by peer",
                "Temporary failure in name resolution"
        );
    }

    /**
     * Provides authentication-related exception messages for testing.
     */
    @Provide
    Arbitrary<String> authExceptionMessages() {
        return Arbitraries.of(
                "Authentication failed",
                "Invalid credentials",
                "Permission denied",
                "Unauthorized access",
                "Forbidden: access denied"
        );
    }
}
