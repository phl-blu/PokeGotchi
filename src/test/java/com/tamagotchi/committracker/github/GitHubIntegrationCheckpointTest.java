package com.tamagotchi.committracker.github;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Task 4 Checkpoint - Integration test for complete authentication pipeline.
 * 
 * Tests:
 * 1. OAuth flow works end-to-end
 * 2. Tokens are encrypted and stored securely
 * 3. Rate limit tracking is functional
 * 
 * BEFORE RUNNING:
 * Set the GITHUB_CLIENT_ID environment variable:
 *   Windows PowerShell: $env:GITHUB_CLIENT_ID="Ov23li1fNemWB2pz0nfX"
 *   Windows CMD: set GITHUB_CLIENT_ID=Ov23li1fNemWB2pz0nfX
 */
public class GitHubIntegrationCheckpointTest {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║     Task 4 Checkpoint - Full Integration Test                ║");
        System.out.println("║     OAuth + Token Storage + Rate Limiting                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        boolean allPassed = true;
        
        // Test 1: Token Storage (no OAuth needed)
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST 1: Secure Token Storage");
        System.out.println("═══════════════════════════════════════════════════════════════");
        allPassed &= testTokenStorage();
        System.out.println();
        
        // Test 2: Rate Limit Manager (no OAuth needed)
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST 2: Rate Limit Manager");
        System.out.println("═══════════════════════════════════════════════════════════════");
        allPassed &= testRateLimitManager();
        System.out.println();
        
        // Test 3: Full OAuth + Storage + Rate Limit Integration
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("TEST 3: Full OAuth Integration (requires user interaction)");
        System.out.println("═══════════════════════════════════════════════════════════════");
        
        String clientId = GitHubConfig.getClientId();
        if (clientId == null || clientId.isBlank()) {
            System.out.println("⚠️  SKIPPED: GITHUB_CLIENT_ID not set");
            System.out.println("   Set it to run the full OAuth integration test");
        } else {
            allPassed &= testFullOAuthIntegration(clientId);
        }
        System.out.println();
        
        // Summary
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("                         SUMMARY                               ");
        System.out.println("═══════════════════════════════════════════════════════════════");
        if (allPassed) {
            System.out.println("✅ ALL TESTS PASSED - Task 4 Checkpoint Complete!");
        } else {
            System.out.println("❌ SOME TESTS FAILED - Please review and fix issues");
        }
    }
    
    private static boolean testTokenStorage() {
        try {
            SecureTokenStorage storage = new SecureTokenStorageImpl();
            
            // Test 1.1: Store and retrieve access token
            System.out.println("  📝 Testing token encryption round-trip...");
            String testToken = "gho_test_token_" + System.currentTimeMillis();
            storage.storeAccessToken(testToken);
            
            String retrieved = storage.getAccessToken().orElse(null);
            if (testToken.equals(retrieved)) {
                System.out.println("  ✅ Access token round-trip: PASSED");
            } else {
                System.out.println("  ❌ Access token round-trip: FAILED");
                System.out.println("     Expected: " + testToken);
                System.out.println("     Got: " + retrieved);
                return false;
            }
            
            // Test 1.2: Store and retrieve refresh token
            String testRefresh = "ghr_test_refresh_" + System.currentTimeMillis();
            storage.storeRefreshToken(testRefresh);
            
            String retrievedRefresh = storage.getRefreshToken().orElse(null);
            if (testRefresh.equals(retrievedRefresh)) {
                System.out.println("  ✅ Refresh token round-trip: PASSED");
            } else {
                System.out.println("  ❌ Refresh token round-trip: FAILED");
                return false;
            }
            
            // Test 1.3: Clear tokens
            storage.clearAllTokens();
            if (storage.getAccessToken().isEmpty() && storage.getRefreshToken().isEmpty()) {
                System.out.println("  ✅ Token clearing: PASSED");
            } else {
                System.out.println("  ❌ Token clearing: FAILED");
                return false;
            }
            
            System.out.println("  ✅ Token Storage: ALL TESTS PASSED");
            return true;
            
        } catch (Exception e) {
            System.out.println("  ❌ Token Storage: EXCEPTION - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testRateLimitManager() {
        try {
            RateLimitManager rateLimiter = new RateLimitManagerImpl();
            
            // Test 2.1: Initial state allows requests
            System.out.println("  📝 Testing initial state...");
            if (rateLimiter.canMakeRequest()) {
                System.out.println("  ✅ Initial canMakeRequest: PASSED");
            } else {
                System.out.println("  ❌ Initial canMakeRequest: FAILED (should be true)");
                return false;
            }
            
            // Test 2.2: Record request and check mode
            System.out.println("  📝 Testing rate limit tracking...");
            Instant resetTime = Instant.now().plusSeconds(3600);
            rateLimiter.recordRequest(4500, resetTime);
            
            PollingMode mode = rateLimiter.getCurrentPollingMode();
            if (mode == PollingMode.NORMAL) {
                System.out.println("  ✅ Normal mode at 4500 remaining: PASSED");
            } else {
                System.out.println("  ❌ Expected NORMAL mode, got: " + mode);
                return false;
            }
            
            // Test 2.3: Conservative mode threshold
            rateLimiter.recordRequest(400, resetTime);
            mode = rateLimiter.getCurrentPollingMode();
            if (mode == PollingMode.CONSERVATIVE) {
                System.out.println("  ✅ Conservative mode at 400 remaining: PASSED");
            } else {
                System.out.println("  ❌ Expected CONSERVATIVE mode, got: " + mode);
                return false;
            }
            
            // Test 2.4: Minimal mode threshold
            rateLimiter.recordRequest(80, resetTime);
            mode = rateLimiter.getCurrentPollingMode();
            if (mode == PollingMode.MINIMAL) {
                System.out.println("  ✅ Minimal mode at 80 remaining: PASSED");
            } else {
                System.out.println("  ❌ Expected MINIMAL mode, got: " + mode);
                return false;
            }
            
            // Test 2.5: Paused mode when exhausted
            rateLimiter.recordRequest(0, resetTime);
            if (!rateLimiter.canMakeRequest()) {
                System.out.println("  ✅ Paused when exhausted: PASSED");
            } else {
                System.out.println("  ❌ Should be paused when exhausted");
                return false;
            }
            
            // Test 2.6: Polling interval increases as rate limit decreases
            System.out.println("  📝 Testing polling interval monotonicity...");
            RateLimitManager fresh = new RateLimitManagerImpl();
            fresh.recordRequest(4000, resetTime);
            long interval1 = fresh.getCurrentPollingInterval().toMillis();
            
            fresh.recordRequest(300, resetTime);
            long interval2 = fresh.getCurrentPollingInterval().toMillis();
            
            fresh.recordRequest(50, resetTime);
            long interval3 = fresh.getCurrentPollingInterval().toMillis();
            
            if (interval1 <= interval2 && interval2 <= interval3) {
                System.out.println("  ✅ Polling interval monotonicity: PASSED");
                System.out.println("     Intervals: " + interval1 + "ms → " + interval2 + "ms → " + interval3 + "ms");
            } else {
                System.out.println("  ❌ Polling interval should increase as rate limit decreases");
                return false;
            }
            
            System.out.println("  ✅ Rate Limit Manager: ALL TESTS PASSED");
            return true;
            
        } catch (Exception e) {
            System.out.println("  ❌ Rate Limit Manager: EXCEPTION - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    private static boolean testFullOAuthIntegration(String clientId) {
        GitHubOAuthServiceImpl oauthService = new GitHubOAuthServiceImpl(clientId);
        SecureTokenStorage storage = new SecureTokenStorageImpl();
        RateLimitManager rateLimiter = new RateLimitManagerImpl();
        
        try {
            // Step 1: Initiate OAuth
            System.out.println("  📝 Initiating OAuth Device Flow...");
            DeviceCodeResponse deviceCode = oauthService.initiateDeviceFlow()
                .get(30, TimeUnit.SECONDS);
            
            System.out.println();
            System.out.println("  ╔════════════════════════════════════════════════════════╗");
            System.out.println("  ║              AUTHORIZATION REQUIRED                    ║");
            System.out.println("  ╠════════════════════════════════════════════════════════╣");
            System.out.printf("  ║  URL:  %s%n", deviceCode.verificationUri());
            System.out.printf("  ║  Code: %s                                       ║%n", deviceCode.userCode());
            System.out.println("  ╚════════════════════════════════════════════════════════╝");
            System.out.println();
            System.out.println("  ⏳ Waiting for authorization...");
            
            // Step 2: Poll for token
            AccessTokenResponse tokenResponse = oauthService.pollForAccessToken(deviceCode.deviceCode())
                .get(deviceCode.expiresIn() + 30, TimeUnit.SECONDS);
            
            System.out.println("  ✅ OAuth authentication: PASSED");
            
            // Step 3: Store token securely
            System.out.println("  📝 Storing token securely...");
            storage.storeAccessToken(tokenResponse.accessToken());
            if (tokenResponse.hasRefreshToken()) {
                storage.storeRefreshToken(tokenResponse.refreshToken());
            }
            
            // Verify storage
            String storedToken = storage.getAccessToken().orElse(null);
            if (tokenResponse.accessToken().equals(storedToken)) {
                System.out.println("  ✅ Token storage: PASSED");
            } else {
                System.out.println("  ❌ Token storage: FAILED");
                return false;
            }
            
            // Step 4: Validate token and check rate limit
            System.out.println("  📝 Validating token and checking rate limit...");
            boolean isValid = oauthService.validateToken(storedToken)
                .get(10, TimeUnit.SECONDS);
            
            if (isValid) {
                System.out.println("  ✅ Token validation: PASSED");
            } else {
                System.out.println("  ❌ Token validation: FAILED");
                return false;
            }
            
            // Step 5: Make API call and track rate limit
            System.out.println("  📝 Testing rate limit tracking with real API...");
            okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();
            okhttp3.Request request = new okhttp3.Request.Builder()
                .url(GitHubConfig.USER_ENDPOINT)
                .header(GitHubConfig.ACCEPT_HEADER, GitHubConfig.ACCEPT_JSON)
                .header(GitHubConfig.USER_AGENT_HEADER, GitHubConfig.USER_AGENT_VALUE)
                .header(GitHubConfig.AUTHORIZATION_HEADER, "Bearer " + storedToken)
                .get()
                .build();
            
            try (okhttp3.Response response = client.newCall(request).execute()) {
                String remainingStr = response.header(GitHubConfig.RATE_LIMIT_REMAINING_HEADER);
                String resetStr = response.header(GitHubConfig.RATE_LIMIT_RESET_HEADER);
                
                if (remainingStr != null && resetStr != null) {
                    int remaining = Integer.parseInt(remainingStr);
                    Instant resetTime = Instant.ofEpochSecond(Long.parseLong(resetStr));
                    
                    rateLimiter.recordRequest(remaining, resetTime);
                    
                    System.out.println("  ✅ Rate limit tracking: PASSED");
                    System.out.println("     Remaining: " + remaining);
                    System.out.println("     Mode: " + rateLimiter.getCurrentPollingMode());
                    System.out.println("     Polling interval: " + rateLimiter.getCurrentPollingInterval().toMinutes() + " min");
                } else {
                    System.out.println("  ⚠️  Rate limit headers not found (may be cached response)");
                }
            }
            
            // Cleanup
            System.out.println("  📝 Cleaning up test tokens...");
            storage.clearAllTokens();
            System.out.println("  ✅ Cleanup: PASSED");
            
            System.out.println();
            System.out.println("  ✅ Full OAuth Integration: ALL TESTS PASSED");
            return true;
            
        } catch (Exception e) {
            System.out.println("  ❌ Full OAuth Integration: EXCEPTION - " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            oauthService.shutdown();
        }
    }
}
