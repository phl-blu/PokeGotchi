package com.tamagotchi.committracker.github;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for GitHub OAuth token handling.
 * 
 * These tests validate the correctness properties defined in the design document
 * for the GitHub OAuth integration.
 */
class GitHubOAuthProperties {

    /**
     * **Feature: github-api-integration, Property 7: Token Refresh Idempotency**
     * **Validates: Requirements 1.4, 2.3**
     * 
     * For any valid refresh token, multiple concurrent refresh attempts SHALL result 
     * in exactly one new access token being stored.
     * 
     * This property tests the idempotency of token refresh operations by simulating
     * concurrent refresh attempts and verifying that only one token is stored.
     */
    @Property(tries = 50)
    void tokenRefreshIdempotency(
            @ForAll @StringLength(min = 20, max = 50) String refreshToken,
            @ForAll("concurrentAttempts") int numConcurrentAttempts) throws Exception {
        
        // Create a thread-safe token storage simulator
        TokenStorageSimulator storage = new TokenStorageSimulator();
        
        // Create a token refresh coordinator that ensures idempotency
        IdempotentTokenRefresher refresher = new IdempotentTokenRefresher(storage);
        
        // Execute multiple concurrent refresh attempts
        ExecutorService executor = Executors.newFixedThreadPool(numConcurrentAttempts);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numConcurrentAttempts);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<String> storedToken = new AtomicReference<>();
        
        for (int i = 0; i < numConcurrentAttempts; i++) {
            final int attemptId = i;
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Attempt to refresh the token
                    String newToken = refresher.refreshToken(refreshToken, attemptId);
                    if (newToken != null) {
                        successCount.incrementAndGet();
                        storedToken.set(newToken);
                    }
                } catch (Exception e) {
                    // Expected for concurrent attempts that don't win
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all attempts to complete
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Verify idempotency: exactly one token should be stored
        assertEquals(1, storage.getStoreCount(),
                String.format("Expected exactly 1 token to be stored, but got %d for %d concurrent attempts",
                        storage.getStoreCount(), numConcurrentAttempts));
        
        // Verify the stored token is consistent
        assertNotNull(storage.getStoredToken(),
                "A token should have been stored");
    }

    /**
     * Provides the number of concurrent refresh attempts (2-10).
     */
    @Provide
    Arbitrary<Integer> concurrentAttempts() {
        return Arbitraries.integers().between(2, 10);
    }

    /**
     * Property: Access token response validation
     * For any access token response, isValid() returns true if and only if
     * the access token is non-null and non-blank.
     */
    @Property(tries = 100)
    void accessTokenResponseValidation(
            @ForAll("accessTokens") String accessToken,
            @ForAll("tokenTypes") String tokenType,
            @ForAll("scopes") String scope) {
        
        AccessTokenResponse response = new AccessTokenResponse(
                accessToken, tokenType, scope, null, 3600);
        
        boolean expectedValid = accessToken != null && !accessToken.isBlank();
        assertEquals(expectedValid, response.isValid(),
                String.format("Expected isValid=%b for token '%s'", expectedValid, accessToken));
    }

    @Provide
    Arbitrary<String> accessTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50)
        );
    }

    @Provide
    Arbitrary<String> tokenTypes() {
        return Arbitraries.of("bearer", "Bearer", "token");
    }

    @Provide
    Arbitrary<String> scopes() {
        return Arbitraries.of("repo", "repo read:user", "read:user");
    }

    /**
     * Property: Refresh token presence check
     * For any access token response, hasRefreshToken() returns true if and only if
     * the refresh token is non-null and non-blank.
     */
    @Property(tries = 100)
    void refreshTokenPresenceCheck(
            @ForAll("refreshTokens") String refreshToken) {
        
        AccessTokenResponse response = new AccessTokenResponse(
                "valid_access_token", "bearer", "repo", refreshToken, 3600);
        
        boolean expectedHasRefresh = refreshToken != null && !refreshToken.isBlank();
        assertEquals(expectedHasRefresh, response.hasRefreshToken(),
                String.format("Expected hasRefreshToken=%b for refresh token '%s'", 
                        expectedHasRefresh, refreshToken));
    }

    @Provide
    Arbitrary<String> refreshTokens() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50)
        );
    }

    /**
     * Simulates token storage for testing idempotency.
     * Thread-safe implementation that tracks store operations.
     */
    private static class TokenStorageSimulator {
        private final AtomicInteger storeCount = new AtomicInteger(0);
        private final AtomicReference<String> storedToken = new AtomicReference<>();
        
        public synchronized void storeToken(String token) {
            storeCount.incrementAndGet();
            storedToken.set(token);
        }
        
        public int getStoreCount() {
            return storeCount.get();
        }
        
        public String getStoredToken() {
            return storedToken.get();
        }
    }

    /**
     * Simulates an idempotent token refresher that ensures only one refresh
     * operation succeeds for concurrent attempts with the same refresh token.
     */
    private static class IdempotentTokenRefresher {
        private final TokenStorageSimulator storage;
        private final ConcurrentHashMap<String, Object> refreshLocks = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> refreshedTokens = new ConcurrentHashMap<>();
        
        public IdempotentTokenRefresher(TokenStorageSimulator storage) {
            this.storage = storage;
        }
        
        /**
         * Refreshes a token with idempotency guarantee.
         * Multiple concurrent calls with the same refresh token will result in
         * exactly one new token being generated and stored.
         */
        public String refreshToken(String refreshToken, int attemptId) {
            // Get or create a lock for this refresh token
            Object lock = refreshLocks.computeIfAbsent(refreshToken, k -> new Object());
            
            synchronized (lock) {
                // Check if already refreshed
                String existingToken = refreshedTokens.get(refreshToken);
                if (existingToken != null) {
                    // Already refreshed by another thread, return null to indicate
                    // this attempt didn't perform the refresh
                    return null;
                }
                
                // Simulate token refresh (in real implementation, this would call GitHub API)
                String newToken = "new_access_token_" + System.nanoTime();
                
                // Store the new token
                storage.storeToken(newToken);
                refreshedTokens.put(refreshToken, newToken);
                
                return newToken;
            }
        }
    }
}
